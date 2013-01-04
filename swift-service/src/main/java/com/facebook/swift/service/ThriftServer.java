/*
 * Copyright (C) 2012 Facebook, Inc.
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
package com.facebook.swift.service;

import com.facebook.nifty.core.NettyConfigBuilder;
import com.facebook.nifty.core.NettyServerTransport;
import com.facebook.nifty.core.ThriftServerDef;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.weakref.jmx.Managed;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class ThriftServer implements Closeable
{
    private enum State {
        NOT_STARTED,
        RUNNING,
        CLOSED,
    }

    private final NettyServerTransport transport;
    private final int workerThreads;
    private final int port;
    private final DefaultChannelGroup allChannels = new DefaultChannelGroup();

    private final ExecutorService acceptorExecutor;
    private final ExecutorService ioExecutor;
    private final ExecutorService workerExecutor;

    private State state = State.NOT_STARTED;

    public ThriftServer(TProcessor processor)
    {
        this(processor, new ThriftServerConfig());
    }

    public ThriftServer(TProcessor processor, ThriftServerConfig config)
    {
        this(processor, config, new HashedWheelTimer());
    }

    @Inject
    public ThriftServer(TProcessor processor, ThriftServerConfig config, @ThriftServerTimer Timer timer)
    {
        TProcessorFactory processorFactory = new TProcessorFactory(processor);

        port = getSpecifiedOrRandomPort(config);

        workerThreads = config.getWorkerThreads();

        workerExecutor = newFixedThreadPool(workerThreads, new ThreadFactoryBuilder().setNameFormat("thrift-worker-%s").build());
        acceptorExecutor = newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("thrift-acceptor-%s").build());
        ioExecutor = newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("thrift-io-%s").build());

        ThriftServerDef thriftServerDef = ThriftServerDef.newBuilder()
                                                         .name("thrift")
                                                         .listen(port)
                                                         .limitFrameSizeTo((int) config.getMaxFrameSize().toBytes())
                                                         .clientIdleTimeout(config.getClientIdleTimeout())
                                                         .withProcessorFactory(processorFactory)
                                                         .using(workerExecutor).build();

        transport = new NettyServerTransport(thriftServerDef, new NettyConfigBuilder(), allChannels, timer);
    }

    private int getSpecifiedOrRandomPort(ThriftServerConfig config)
    {
        if (config.getPort() != 0) {
            return config.getPort();
        }
        try (ServerSocket s = new ServerSocket()) {
            s.bind(new InetSocketAddress(0));
            return s.getLocalPort();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to get a random port", e);
        }
    }

    @Managed
    public int getPort()
    {
        return port;
    }

    @Managed
    public int getWorkerThreads()
    {
        return workerThreads;
    }

    public synchronized boolean isRunning() {
        return state == State.RUNNING;
    }

    @PostConstruct
    public synchronized ThriftServer start()
    {
        Preconditions.checkState(state != State.CLOSED, "Thrift server is closed");
        if (state == State.NOT_STARTED) {
            transport.start(acceptorExecutor, ioExecutor);
            state = State.RUNNING;
        }
        return this;
    }

    @PreDestroy
    @Override
    public synchronized void close()
    {
        if (state == State.CLOSED) {
            return;
        }

        if (state == State.RUNNING) {
            try {
                transport.stop();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // stop bosses
        if (acceptorExecutor != null) {
            shutdownExecutor(acceptorExecutor);
        }

        // stop worker
        if (workerExecutor != null) {
            shutdownExecutor(workerExecutor);
        }

        // TODO: allow an option here to control if we need to drain connections and wait instead of killing them all
        allChannels.close();

        // finally the reader writer
        if (ioExecutor != null) {
            shutdownExecutor(ioExecutor);
        }

        state = State.CLOSED;
    }

    private static void shutdownExecutor(ExecutorService executor)
    {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            //ignored
            Thread.currentThread().interrupt();
        }
    }
}
