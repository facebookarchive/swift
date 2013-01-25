/**
 * Copyright 2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.nifty.core;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ShutdownUtil
{
    private static final Logger log = LoggerFactory.getLogger(ShutdownUtil.class);

    public static void shutdownChannelFactory(ChannelFactory channelFactory,
                                              ExecutorService bossExecutor,
                                              ExecutorService workerExecutor,
                                              ChannelGroup allChannels)
    {
        // Close all channels
        if (allChannels != null) {
            closeChannels(allChannels);
        }

        // Shutdown the channel factory
        if (channelFactory != null) {
            channelFactory.shutdown();
        }

        // Stop boss threads
        if (bossExecutor != null) {
            shutdownExecutor(bossExecutor, "bossExecutor");
        }

        // Finally stop I/O workers
        if (workerExecutor != null) {
            shutdownExecutor(workerExecutor, "workerExecutor");
        }
    }

    public static void closeChannels(ChannelGroup allChannels)
    {
        if (allChannels.size() > 0)
        {
            // TODO : allow an option here to control if we need to drain connections and wait instead of killing them all
            try {
                log.info("Closing " + allChannels.size() + " open client connections");
                if (!allChannels.close().await(5, TimeUnit.SECONDS)) {
                    log.warn("Failed to close all open client connections");
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while closing client connections");
                Thread.currentThread().interrupt();
            }
        }
    }

    // TODO : make wait time configurable ?
    public static void shutdownExecutor(ExecutorService executor, final String name)
    {
        executor.shutdown();
        try {
            log.info("Waiting for {} to shutdown", name);
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("{} did not shutdown properly", name);
            }
        }
        catch (InterruptedException e) {
            log.warn("Interrupted while waiting for {} to shutdown", name);
            Thread.currentThread().interrupt();
        }
    }
}
