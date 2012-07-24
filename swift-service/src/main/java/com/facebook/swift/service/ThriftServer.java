/**
 * Copyright 2012 Facebook, Inc.
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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class ThriftServer implements Closeable
{
    private final NettyServerTransport transport;
    private final int acceptorThreads;
    private final int workerThreads;
    private final int port;

    private ExecutorService bossExecutor;
    private ExecutorService workerExecutor;

    public ThriftServer(TProcessor processor)
    {
        this (processor, new ThriftServerConfig());
    }

    @Inject
    public ThriftServer(TProcessor processor, ThriftServerConfig config)
    {
        TProcessorFactory processorFactory = new TProcessorFactory(processor);

        port = getSpecifiedOrRandomPort(config);

        ThriftServerDef thriftServerDef = new ThriftServerDef(
                "thrift",
                port,
                (int) config.getMaxFrameSize().toBytes(),
                processorFactory,
                new TBinaryProtocol.Factory(),
                new TBinaryProtocol.Factory(),
                false,
                MoreExecutors.sameThreadExecutor()
        );

        acceptorThreads = config.getAcceptorThreads();
        workerThreads = config.getWorkerThreads();
        transport = new NettyServerTransport(thriftServerDef, new NettyConfigBuilder(), new DefaultChannelGroup());
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

    public int getPort()
    {
        return port;
    }

    @PostConstruct
    public ThriftServer start()
    {
        bossExecutor = newFixedThreadPool(acceptorThreads, new ThreadFactoryBuilder().setNameFormat("thrift-acceptor-%s").build());
        workerExecutor = newFixedThreadPool(workerThreads, new ThreadFactoryBuilder().setNameFormat("thrift-worker-%s").build());
        transport.start(bossExecutor, workerExecutor);
        return this;
    }

    @PreDestroy
    @Override
    public void close()
    {
        try {
            transport.stop();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // stop bosses
        if (bossExecutor != null) {
            shutdownExecutor(bossExecutor);
            bossExecutor = null;
        }

        // finally the reader writer
        if (workerExecutor != null) {
            shutdownExecutor(workerExecutor);
            workerExecutor = null;
        }
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
