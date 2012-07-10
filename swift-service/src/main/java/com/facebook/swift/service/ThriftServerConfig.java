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

import io.airlift.configuration.Config;
import io.airlift.units.DataSize;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import static io.airlift.units.DataSize.Unit.MEGABYTE;

public class ThriftServerConfig
{
    private int port;
    private DataSize maxFrameSize = new DataSize(1, MEGABYTE);
    private int acceptorThreads = Runtime.getRuntime().availableProcessors();
    private int workerThreads = 200;

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
    public int getAcceptorThreads()
    {
        return acceptorThreads;
    }

    @Config("thrift.acceptor-threads.max")
    public ThriftServerConfig setAcceptorThreads(int acceptorThreads)
    {
        this.acceptorThreads = acceptorThreads;
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
}
