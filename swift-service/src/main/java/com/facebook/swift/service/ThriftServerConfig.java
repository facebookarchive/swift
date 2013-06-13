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

import io.airlift.configuration.Config;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import static io.airlift.units.DataSize.Unit.MEGABYTE;

public class ThriftServerConfig
{
    private static final int DEFAULT_BOSS_THREAD_COUNT = 1;
    private static final int DEFAULT_IO_WORKER_THREAD_COUNT = 2 * Runtime.getRuntime().availableProcessors();

    private int port;
    private int workerThreads = 200;
    private int acceptorThreadCount = DEFAULT_BOSS_THREAD_COUNT;
    private int ioThreadCount = DEFAULT_IO_WORKER_THREAD_COUNT;
    private Duration clientIdleTimeout;

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

    @Config("thrift.max-frame-size")
    public ThriftServerConfig setMaxFrameSize(DataSize maxFrameSize)
    {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    @Min(1)
    public int getWorkerThreads()
    {
        return workerThreads;
    }

    @Config("thrift.threads.max")
    public ThriftServerConfig setWorkerThreads(int workerThreads)
    {
        this.workerThreads = workerThreads;
        return this;
    }

    public Duration getClientIdleTimeout()
    {
        return this.clientIdleTimeout;
    }

    @Config("thrift.client-idle-timeout")
    public ThriftServerConfig setClientIdleTimeout(Duration clientIdleTimeout)
    {
        this.clientIdleTimeout = clientIdleTimeout;
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
}
