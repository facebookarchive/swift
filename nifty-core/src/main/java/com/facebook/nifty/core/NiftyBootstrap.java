/*
 * Copyright (C) 2012-2013 Facebook, Inc.
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
package com.facebook.nifty.core;

import com.google.inject.Inject;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A lifecycle object that manages starting up and shutting down multiple core channels.
 */
public class NiftyBootstrap
{
    private static final Logger log = LoggerFactory.getLogger(NiftyBootstrap.class);

    private final ChannelGroup allChannels;
    private ArrayList<NettyServerTransport> transports;
    private ExecutorService bossExecutor;
    private ExecutorService workerExecutor;
    private final Timer timer;

    /**
     * This takes a Set of ThriftServerDef. Use Guice Multibinder to inject.
     */
    @Inject
    public NiftyBootstrap(
            Set<ThriftServerDef> thriftServerDefs,
            NettyConfigBuilder configBuilder,
            ChannelGroup allChannels)
    {
        this.allChannels = allChannels;
        this.transports = new ArrayList<>();
        this.timer = new HashedWheelTimer();
        for (ThriftServerDef thriftServerDef : thriftServerDefs) {
            transports.add(new NettyServerTransport(thriftServerDef,
                                                    configBuilder,
                                                    allChannels,
                                                    timer));
        }

    }

    @PostConstruct
    public void start()
    {
        bossExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("nifty-boss"));
        workerExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("nifty-worker"));
        for (NettyServerTransport transport : transports) {
            transport.start(bossExecutor, workerExecutor);
        }
    }

    @PreDestroy
    public void stop()
    {
        for (NettyServerTransport transport : transports) {
            try {
                transport.stop();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // stop bosses
        if (bossExecutor != null) {
            shutdownExecutor(bossExecutor, "bossExecutor");
            bossExecutor = null;
        }

        // TODO : allow an option here to control if we need to drain connections and wait instead of killing them all

        try {
            allChannels.close();
        }
        catch (Exception e) {
            log.warn("ignored exception while shutting down channels", e);
        }

        // finally the reader writer
        if (workerExecutor != null) {
            shutdownExecutor(workerExecutor, "workerExecutor");
            workerExecutor = null;
        }
    }

    // TODO : make wait time configurable ?
    static void shutdownExecutor(ExecutorService executor, final String name)
    {
        executor.shutdown();
        try {
            log.info("waiting for {} to shutdown", name);
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            //ignored
            Thread.currentThread().interrupt();
        }
    }

    public static class NamedThreadFactory implements ThreadFactory
    {
        private final String baseName;
        private final AtomicInteger threadNum = new AtomicInteger(0);

        public NamedThreadFactory(String baseName)
        {
            this.baseName = baseName;
        }

        @Override
        public synchronized Thread newThread(Runnable r)
        {
            Thread t = Executors.defaultThreadFactory().newThread(r);

            t.setName(baseName + "-" + threadNum.getAndIncrement());

            return t;
        }
    }
}
