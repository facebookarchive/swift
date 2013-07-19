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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.airlift.configuration.Config;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;

import java.util.concurrent.ExecutorService;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import static io.airlift.units.DataSize.Unit.MEGABYTE;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class ThriftServerConfig
{
    private static final int DEFAULT_BOSS_THREAD_COUNT = 1;
    private static final int DEFAULT_IO_WORKER_THREAD_COUNT = 2 * Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_WORKER_THREAD_COUNT = 200;

    private int port;
    private int connectionLimit;
    private int acceptorThreadCount = DEFAULT_BOSS_THREAD_COUNT;
    private int ioThreadCount = DEFAULT_IO_WORKER_THREAD_COUNT;
    private Duration clientIdleTimeout;
    private Optional<Integer> workerThreads = Optional.absent();
    private Optional<ExecutorService> workerExecutor = Optional.absent();

    /**
     * The default maximum allowable size for a single incoming thrift request or outgoing thrift
     * response. A server can configure the actual maximum to be much higher (up to 0x7FFFFFFF or
     * almost 2 GB). This default could also be safely bumped up, but 64MB is chosen simply
     * because it seems reasonable that if you are sending requests or responses larger than
     * that, it should be a conscious decision (something you must manually configure).
     */
    private DataSize maxFrameSize = new DataSize(64, MEGABYTE);

    @Min(0)
    @Max(65535)
    public int getPort()
    {
        return port;
    }

    @Config("thrift.port")
    public ThriftServerConfig setPort(int port)
    {
        this.port = port;
        return this;
    }

    public DataSize getMaxFrameSize()
    {
        return maxFrameSize;
    }

    /**
     * Sets a maximum frame size
     * @param maxFrameSize
     * @return
     */
    @Config("thrift.max-frame-size")
    public ThriftServerConfig setMaxFrameSize(DataSize maxFrameSize)
    {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    @Min(1)
    public int getWorkerThreads()
    {
        return workerThreads.or(DEFAULT_WORKER_THREAD_COUNT);
    }

    /**
     * Sets the number of worker threads that will be created for processing thrift requests after
     * they have arrived. Any value passed here will be ignored if
     * {@link ThriftServerConfig#setWorkerExecutor(java.util.concurrent.ExecutorService)} is called.
     *
     * The default value is 200.
     *
     * @param workerThreads Number of worker threads to use
     * @return This {@link ThriftServerConfig} instance
     */
    @Config("thrift.threads.max")
    public ThriftServerConfig setWorkerThreads(int workerThreads)
    {
        this.workerThreads = Optional.of(workerThreads);
        return this;
    }

    public int getAcceptorThreadCount()
    {
        return acceptorThreadCount;
    }

    @Config("thrift.acceptor-threads.count")
    public ThriftServerConfig setAcceptorThreadCount(int acceptorThreadCount)
    {
        this.acceptorThreadCount = acceptorThreadCount;
        return this;
    }

    public int getIoThreadCount()
    {
        return ioThreadCount;
    }

    @Config("thrift.io-threads.count")
    public ThriftServerConfig setIoThreadCount(int ioThreadCount)
    {
        this.ioThreadCount = ioThreadCount;
        return this;
    }

    public Duration getClientIdleTimeout()
    {
        return this.clientIdleTimeout;
    }

    /**
     * Sets a timeout period between receiving requests from a client connection. If the timeout
     * is exceeded (no complete requests have arrived from the client within the timeout), the
     * server will disconnect the idle client.
     *
     * The default is 60s.
     *
     * @param clientIdleTimeout The timeout
     * @return This {@link ThriftServerConfig} instance
     */
    @Config("thrift.client-idle-timeout")
    public ThriftServerConfig setClientIdleTimeout(Duration clientIdleTimeout)
    {
        this.clientIdleTimeout = clientIdleTimeout;
        return this;
    }

    @Min(0)
    public int getConnectionLimit()
    {
        return this.connectionLimit;
    }

    /**
     * Sets an upper bound on the number of concurrent connections the server will accept.
     *
     * The default is not to limit the number of connections.
     *
     * @param connectionLimit The maximum number of concurrent connections
     * @return This {@link ThriftServerConfig} instance
     */
    @Config("thrift.server")
    public ThriftServerConfig setConnectionLimit(int connectionLimit)
    {
        this.connectionLimit = connectionLimit;
        return this;
    }

    public ExecutorService getWorkerExecutor()
    {
        return workerExecutor.or(makeDefaultWorkerExecutor());
    }

    /**
     * Sets the executor that will be used to process thrift requests after they arrive. Setting
     * this will override any call to {@link ThriftServerConfig#setWorkerThreads(int)}.
     *
     * The default behavior will be to synthesize a fixed-size {@link java.util.concurrent.ThreadPoolExecutor}
     * using the result of {@link ThriftServerConfig#getWorkerThreads()}
     *
     * @param workerExecutor The worker executor
     * @return This {@link ThriftServerConfig} instance
     */
    public ThriftServerConfig setWorkerExecutor(ExecutorService workerExecutor)
    {
        this.workerExecutor = Optional.of(workerExecutor);
        return this;
    }

    private ExecutorService makeDefaultWorkerExecutor()
    {
        return newFixedThreadPool(getWorkerThreads(), new ThreadFactoryBuilder().setNameFormat("thrift-worker-%s").build());
    }
}
