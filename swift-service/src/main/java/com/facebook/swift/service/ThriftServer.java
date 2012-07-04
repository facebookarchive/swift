/*
 * Copyright 2004-present Facebook. All Rights Reserved.
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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newCachedThreadPool;

public class ThriftServer implements AutoCloseable
{
    private final NettyServerTransport transport;
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

        workerThreads = config.getWorkerThreads();
        transport = new NettyServerTransport(thriftServerDef, new NettyConfigBuilder());
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
        bossExecutor = newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("thrift-boss-%s").build());
        workerExecutor = Executors.newFixedThreadPool(workerThreads, new ThreadFactoryBuilder().setNameFormat("thrift-worker-%s").build());
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
