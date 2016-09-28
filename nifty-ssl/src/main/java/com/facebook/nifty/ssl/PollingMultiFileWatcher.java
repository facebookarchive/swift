/*
 * Copyright (C) 2012-2016 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.nifty.ssl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.airlift.log.Logger;
import io.airlift.units.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * An implementation of {@link MultiFileWatcher} that polls the watched files for changes at regular intervals
 * in a background thread.
 */
public final class PollingMultiFileWatcher implements MultiFileWatcher {
    // Fields set in constructor
    private final long initialDelay;
    private final long interval;
    private final TimeUnit timeUnit;
    private final MutableStats stats;
    // Fields set in start()
    private Consumer<Set<File>> callback;
    private Set<File> watchedFiles;
    private ListeningScheduledExecutorService executorService;
    private ListenableScheduledFuture<?> future;
    // Cache of last known file state, updated at every polling cycle
    private final AtomicReference<Map<File, FileMetadata>> metadataCacheRef;

    private static final Logger log = Logger.get(PollingMultiFileWatcher.class);

    /**
     * Creates a new watcher. The watcher doesn't start scanning files on disk until the {@code start()} method is
     * called.
     *
     * @param initialDelay how long to wait until the first scan of the files.
     * @param interval how often to rescan the files.
     */
    public PollingMultiFileWatcher(Duration initialDelay, Duration interval) {
        this.initialDelay = requireNonNull(initialDelay).toMillis();
        this.interval = requireNonNull(interval).toMillis();
        this.timeUnit = TimeUnit.MILLISECONDS;
        stats = new MutableStats();
        this.executorService = null;
        this.future = null;
        this.metadataCacheRef = new AtomicReference<>(ImmutableMap.of());
        watchedFiles = ImmutableSet.of();
        callback = null;
    }

    /**
     * Starts polling the watched files for changes.
     *
     * @param files a set of one or more files to watch for changes.
     * @param callback the callback to call with the set of files that changed since the last time.
     */
    @Override public void start(Set<File> files, Consumer<Set<File>> callback) {
        if (isStarted()) {
            throw new IllegalStateException("start() should not be called more than once");
        }
        Set<File> filesCopy = ImmutableSet.copyOf(files);
        Preconditions.checkArgument(filesCopy.size() > 0, "must specify at least 1 file to watch for changes");
        this.callback = requireNonNull(callback);
        watchedFiles = filesCopy;
        executorService = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(1));
        future = executorService.scheduleAtFixedRate(
            this::scanFilesForChanges,
            initialDelay,
            interval,
            timeUnit);
    }

    /**
     * @return true if the polling thread has been started and shutdown has not been called.
     */
    @Override public boolean isStarted() {
        return executorService != null;
    }

    /**
     * Stops polling the files for changes. Should be called during server shutdown or when this watcher is no
     * longer needed to make sure the background thread is stopped.
     */
    @Override public void shutdown() {
        if (isStarted()) {
            future.cancel(true);
            executorService.shutdown();
            future = null;
            executorService = null;
            watchedFiles = ImmutableSet.of();
            callback = null;
            metadataCacheRef.set(ImmutableMap.of());
            stats.clear();
        }
    }

    /**
     * @return a {@link Stats} object with stat counters.
     */
    public Stats getStats() {
        return new Stats(stats);
    }

    @Override protected void finalize() {
        // Implement finalize() so we'll try to stop the background thread if caller forgets to do it.
        // JVM provides no guarantees about when (or even if) the finalizer will run, so don't rely on it.
        // Effective Java Item 7 - warn if a "safety net" finalizer actually does any work.
        if (isStarted()) {
            log.warn("%s garbage-collected but shutdown() was never called. " +
                    "Don't rely on finalizers to clean up background threads.",
                    this.getClass().getSimpleName());
            shutdown();
        }
    }

    /**
     * Scans the watched files for changes. If any changes are detected, calls the user callback with the
     * set of all files that were modified since the last successful update attempt. Note that when a watched
     * file is deleted, it will not be considered modified.
     *
     * Changes are tracked by watching certain file attributes (mtime, ctime, inode) and SHA-256 hashes of file
     * contents and comparing against last known values.
     *
     * If a file is deleted or an I/O or permission error occurs while trying to stat or read it, the error is
     * ignored, but the file's metadata is removed from the metadata cache so next time it is read successfully it
     * will be considered an update.
     */
    private void scanFilesForChanges() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            // This should never happen, all JVM implementations must support SHA-256 according to Oracle docs.
            throw new RuntimeException(e);
        }
        ImmutableSet.Builder<File> modifiedFilesBuilder = ImmutableSet.builder();
        Map<File, FileMetadata> metadataCache = Maps.newHashMap(metadataCacheRef.get());
        for (File file : watchedFiles) {
            try {
                FileMetadata meta = new FileMetadata(file, digest);
                if (!meta.equals(metadataCache.get(file))) {
                    metadataCache.put(file, meta);
                    modifiedFilesBuilder.add(file);
                }
            }
            catch (IOException | SecurityException e) {
                // I/O error, file not found, or access to file not allowed
                log.warn(
                    "Error trying to stat or read file %s: %s: %s",
                    file.toString(),
                    e.getClass().getName(),
                    e.getMessage());
                metadataCache.remove(file);
            }
        }

        // We need to swallow exceptions from the user callback, otherwise an uncaught exception could kill the
        // poller thread.
        Set<File> modifiedFiles = modifiedFilesBuilder.build();
        boolean callbackCalled = false;
        boolean callbackSucceeded = false;
        try {
            if (!modifiedFiles.isEmpty()) {
                callbackCalled = true;
                callback.accept(modifiedFiles);
                callbackSucceeded = true;
            }
            // Only update the metadata cache if all callbacks succeeded.
            metadataCacheRef.set(ImmutableMap.copyOf(metadataCache));
        }
        catch (Exception e) {
            log.warn("Error from user callback: %s: %s", e.getClass().toString(), e.getMessage());
        }
        // Update stats
        if (!modifiedFiles.isEmpty()) {
            stats.fileChangesDetected.getAndAdd(modifiedFiles.size());
        }
        if (callbackCalled) {
            stats.callbacksInvoked.getAndIncrement();
            if (callbackSucceeded) {
                stats.callbacksSucceeded.getAndIncrement();
            }
            else {
                stats.callbacksFailed.getAndIncrement();
            }
        }
        stats.pollCycles.getAndIncrement();
    }

    /**
     * Computes a hash of the given file's contents using the provided MessageDigest.
     *
     * @param file the file.
     * @param md the message digest.
     * @return the hash of the file contents.
     * @throws IOException if the file contents cannot be read.
     */
    private static String computeFileHash(File file, MessageDigest md) throws IOException {
        md.reset();
        return BaseEncoding.base16().encode(md.digest(Files.toByteArray(file)));
    }

    /**
     * Mutable polling statistics object. Thread safe.
     */
    private static final class MutableStats {
        private final AtomicLong callbacksFailed;
        private final AtomicLong callbacksInvoked;
        private final AtomicLong callbacksSucceeded;
        private final AtomicLong fileChangesDetected;
        private final AtomicLong pollCycles;

        /**
         * Creates a new Stats object with all-0 values.
         */
        private MutableStats() {
            callbacksFailed = new AtomicLong(0L);
            callbacksInvoked = new AtomicLong(0L);
            callbacksSucceeded = new AtomicLong(0L);
            fileChangesDetected = new AtomicLong(0L);
            pollCycles = new AtomicLong(0L);
        }

        /**
         * Resets the stats object.
         */
        private void clear() {
            callbacksFailed.set(0L);
            callbacksInvoked.set(0L);
            callbacksSucceeded.set(0L);
            fileChangesDetected.set(0L);
            pollCycles.set(0L);
        }
    }

    /**
     * Immutable version of polling statistics.
     */
    public static final class Stats {
        private final long callbacksFailed;
        private final long callbacksInvoked;
        private final long callbacksSucceeded;
        private final long fileChangesDetected;
        private final long pollCycles;

        /**
         * Creates a new Stats object with all-0 values.
         */
        private Stats(MutableStats mutableStats) {
            callbacksFailed = mutableStats.callbacksFailed.get();
            callbacksInvoked = mutableStats.callbacksInvoked.get();
            callbacksSucceeded = mutableStats.callbacksSucceeded.get();
            fileChangesDetected = mutableStats.fileChangesDetected.get();
            pollCycles = requireNonNull(mutableStats).pollCycles.get();
        }

        /**
         * @return number of times that the user callback was called, but threw an exception.
         */
        public long getCallbacksFailed() {
            return callbacksFailed;
        }

        /**
         * @return number of times that the user callback was called.
         */
        public long getCallbacksInvoked() {
            return callbacksInvoked;
        }

        /**
         * @return number of times that the user callback was called and returned without throwing.
         */
        public long getCallbacksSucceeded() {
            return callbacksSucceeded;
        }

        /**
         * @return number of times a file change was detected.
         */
        public long getFileChangesDetected() {
            return fileChangesDetected;
        }

        /**
         * @return number of times the polling method ran to check for file changes.
         */
        public long getPollCycles() {
            return pollCycles;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            else if (!(obj instanceof Stats)) {
                return false;
            }
            Stats that = (Stats) obj;
            return callbacksFailed == that.callbacksFailed &&
                callbacksInvoked == that.callbacksInvoked &&
                callbacksSucceeded == that.callbacksSucceeded &&
                fileChangesDetected == that.fileChangesDetected &&
                pollCycles == that.pollCycles;
        }

        @Override
        public int hashCode() {
            return Objects.hash(callbacksFailed,
                callbacksInvoked,
                callbacksSucceeded,
                fileChangesDetected,
                pollCycles);
        }
    }

    /**
     * Encapsulates some known metadata about a file. This is used to detect file changes. We consider two
     * metadata objects to be equal when the file path, creation time, modification time, inode, and contents of
     * the two files are equals. If any of these things change, we will consider the file updated and call the
     * {@code onFilesUpdated()} callback.
     *
     * Tracking only mtime is insufficient: some file systems may not support it, it often has a coarse
     * granularity (1 second on Linux at the time of this writing), and it can be explicitly set to any value by
     * users, including the old value even when file contents have changed.
     *
     * Tracking only file contents would probably be good enough in practice, but we want to allow triggering the
     * update callback even when the contents have not changed (for testing) with a simple command like
     * {@code touch /path/to/file}, which changes the mtime but not the contents.
     */
    private static final class FileMetadata {
        private final File filePath;
        private final FileTime ctime;
        private final FileTime mtime;
        private final Object fileKey;
        private final String contentsHash;

        FileMetadata(File file, FileTime ctime, FileTime mtime, Object fileKey, String contentsHash) {
            this.filePath = requireNonNull(file);
            this.ctime = requireNonNull(ctime);
            this.mtime = requireNonNull(mtime);
            this.fileKey = fileKey; // not necessarily supported on all file systems and may be null per JavaDocs.
            this.contentsHash = requireNonNull(contentsHash);
        }

        FileMetadata(File file, BasicFileAttributes attrs, String contentsHash) {
            this(file, attrs.creationTime(), attrs.lastModifiedTime(), attrs.fileKey(), contentsHash);
        }

        FileMetadata(File file, MessageDigest digest) throws IOException {
            this(file,
                java.nio.file.Files.readAttributes(file.toPath(), BasicFileAttributes.class),
                computeFileHash(file, digest));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            else if (!(obj instanceof FileMetadata)) {
                return false;
            }
            FileMetadata that = (FileMetadata) obj;
            return Objects.equals(filePath, that.filePath) &&
                Objects.equals(ctime, that.ctime) &&
                Objects.equals(mtime, that.mtime) &&
                Objects.equals(fileKey, that.fileKey) &&
                Objects.equals(contentsHash, that.contentsHash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filePath, ctime, mtime, fileKey, contentsHash);
        }
    }
}
