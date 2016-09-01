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

import com.google.common.collect.ImmutableSet;
import io.airlift.units.Duration;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class PollingMultiFileWatcherTest {
    private static class TestWatcher {
        private final Set<File> filesToWatch;
        private final PollingMultiFileWatcher watcher;
        private final AtomicReference<Set<File>> updatedFiles;

        TestWatcher(Set<File> filesToWatch, Duration initialDelay, Duration interval) {
            this.filesToWatch = ImmutableSet.copyOf(filesToWatch);
            watcher = new PollingMultiFileWatcher(initialDelay, interval);
            updatedFiles = new AtomicReference<>(ImmutableSet.of());
        }

        private void onFilesUpdated(Set<File> modifiedFiles) {
            updatedFiles.set(modifiedFiles);
        }

        Set<File> getUpdatedFilesSinceLastTime() {
            Set<File> emptySet = ImmutableSet.of();
            return updatedFiles.getAndSet(emptySet);
        }

        void start() {
            watcher.start(filesToWatch, this::onFilesUpdated);
        }

        void shutdown() {
            watcher.shutdown();
        }

        PollingMultiFileWatcher.Stats getStats() {
            return watcher.getStats();
        }
    }

    private File file1 = null;
    private File file2 = null;
    private TestWatcher poller = null;

    @BeforeMethod
    public void setUp() throws InterruptedException, IOException {
        file1 = File.createTempFile("abstract_multi_file_poller_test", "file1");
        Files.write(file1.toPath(), new byte[]{1}, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        file2 = File.createTempFile("abstract_multi_file_poller_test", "file2");
        Files.write(file2.toPath(), new byte[]{2}, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        poller = new TestWatcher(ImmutableSet.of(file1, file2),
                                 Duration.valueOf("0 ms"),
                                 Duration.valueOf("100 ms"));
        long callbacksSucceeded = poller.getStats().getCallbacksSucceeded();
        poller.start();
        while (poller.getStats().getCallbacksSucceeded() < callbacksSucceeded + 1) {
            Thread.sleep(25);
        }
        Assert.assertEquals(poller.getUpdatedFilesSinceLastTime(), ImmutableSet.of(file1, file2));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws IOException {
        if (poller != null) {
            poller.shutdown();
            poller = null;
        }
        if (file1 != null) {
            Files.deleteIfExists(file1.toPath());
            file1 = null;
        }
        if (file2 != null) {
            Files.deleteIfExists(file2.toPath());
            file2 = null;
        }
    }

    @Test
    public void testSingleFileUpdate() throws InterruptedException, IOException {
        long fileChanges = poller.getStats().getFileChangesDetected();
        Files.write(file1.toPath(), new byte[]{11}, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        while (fileChanges == poller.getStats().getFileChangesDetected()) {
            Thread.sleep(25);
        }
        Assert.assertEquals(poller.getUpdatedFilesSinceLastTime(), ImmutableSet.of(file1));
    }

    @Test
    public void testMultiFileUpdate() throws InterruptedException, IOException {
        long fileChanges = poller.getStats().getFileChangesDetected();
        Files.write(file1.toPath(), new byte[]{11}, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.write(file2.toPath(), new byte[]{22}, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        while (poller.getStats().getFileChangesDetected() < fileChanges + 2) {
            Thread.sleep(25);
        }
        Assert.assertEquals(poller.getUpdatedFilesSinceLastTime(), ImmutableSet.of(file1, file2));
    }

    @Test
    public void testNoChanges() throws InterruptedException, IOException {
        long pollCycles = poller.getStats().getPollCycles();
        while (poller.getStats().getPollCycles() == pollCycles) {
            Thread.sleep(25);
        }
        Assert.assertEquals(poller.getUpdatedFilesSinceLastTime().size(), 0);
    }

    @Test
    public void testDelete() throws InterruptedException, IOException {
        // When a file is deleted, we should not get an update about it
        long pollCycles = poller.getStats().getPollCycles();
        Files.deleteIfExists(file2.toPath());
        while (pollCycles == poller.getStats().getPollCycles()) {
            Thread.sleep(25);
        }
        Assert.assertEquals(poller.getUpdatedFilesSinceLastTime().size(), 0);
    }
}
