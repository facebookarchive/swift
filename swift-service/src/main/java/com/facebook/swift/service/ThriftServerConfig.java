/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import io.airlift.configuration.Config;
import io.airlift.units.DataSize;

import javax.validation.constraints.Min;

import static io.airlift.units.DataSize.Unit.MEGABYTE;

public class ThriftServerConfig
{
    private int port;
    private DataSize maxFrameSize = new DataSize(1, MEGABYTE);
    private int workerThreads = 200;

    @Min(0)
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
}
