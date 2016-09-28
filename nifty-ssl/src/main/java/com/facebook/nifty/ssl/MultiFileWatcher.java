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

import java.io.File;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An interface for classes that watch a set of file for changes and call a user-specified callback when any of
 * the watched files change.
 */
interface MultiFileWatcher {
    /**
     * Starts the watcher. When any of the provided files change, the provided callback will be called with the
     * files that changed since the last time the callback was invoked. It is up to the implementation how the
     * watched files are monitored for changes, and how much time passes between the file change and the callback
     * invocation. For example, a polling watcher such as {@link PollingMultiFileWatcher} will not call the user
     * callback until the next poll cycle, while a watcher that uses file system event listeners might respond to
     * changes much more quickly.
     *
     * @param files a set of one or more files to watch for changes.
     * @param callback the callback to call with the set of files that changed since the last time. The set of
     *                 modified files passed to the callback should never be null or empty.
     */
    void start(Set<File> files, Consumer<Set<File>> callback);

    /**
     * Stops the watcher.
     */
    void shutdown();

    /**
     * Checks if the watcher has been start()'ed and has not been shutdown().
     * @return true if and only if start() has been called and shutdown() has not been called.
     */
    boolean isStarted();
}
