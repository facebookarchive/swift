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

import com.google.common.net.HostAndPort;
import io.airlift.configuration.Config;
import io.airlift.units.Duration;
import io.airlift.units.MinDuration;

import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import static com.google.common.base.Preconditions.checkArgument;

public class ThriftClientConfig
{
    public static final Duration DEFAULT_CONNECT_TIMEOUT = new Duration(500, TimeUnit.MILLISECONDS);
    public static final Duration DEFAULT_RECEIVE_TIMEOUT = new Duration(1, TimeUnit.MINUTES);
    public static final Duration DEFAULT_READ_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    public static final Duration DEFAULT_WRITE_TIMEOUT = new Duration(1, TimeUnit.MINUTES);
    // Default max frame size of 16 MB
    public static final int DEFAULT_MAX_FRAME_SIZE = 16777216;

    private int maxFrameSize = DEFAULT_MAX_FRAME_SIZE;
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;
    private Duration writeTimeout = DEFAULT_WRITE_TIMEOUT;
    private HostAndPort socksProxy;

    @MinDuration("1ms")
    public Duration getConnectTimeout()
    {
        return connectTimeout;
    }

    @Config("thrift.client.connect-timeout")
    public ThriftClientConfig setConnectTimeout(Duration connectTimeout)
    {
        this.connectTimeout = connectTimeout;
        return this;
    }

    @MinDuration("1ms")
    public Duration getReceiveTimeout()
    {
        return receiveTimeout;
    }

    @Config("thrift.client.receive-timeout")
    public ThriftClientConfig setReceiveTimeout(Duration receiveTimeout)
    {
        this.receiveTimeout = receiveTimeout;
        return this;
    }

    @MinDuration("1ms")
    public Duration getReadTimeout()
    {
        return readTimeout;
    }

    @Config("thrift.client.read-timeout")
    public ThriftClientConfig setReadTimeout(Duration readTimeout)
    {
        this.readTimeout = readTimeout;
        return this;
    }

    @MinDuration("1ms")
    public Duration getWriteTimeout()
    {
        return writeTimeout;
    }

    @Config("thrift.client.write-timeout")
    public ThriftClientConfig setWriteTimeout(Duration writeTimeout)
    {
        this.writeTimeout = writeTimeout;
        return this;
    }

    public HostAndPort getSocksProxy()
    {
        return socksProxy;
    }

    @Config("thrift.client.socks-proxy")
    public ThriftClientConfig setSocksProxy(HostAndPort socksProxy)
    {
        this.socksProxy = socksProxy;
        return this;
    }

    @Min(0)
    @Max(0x3FFFFFFF)
    public int getMaxFrameSize()
    {
        return maxFrameSize;
    }

    @Config("thrift.client.max-frame-size")
    public ThriftClientConfig setMaxFrameSize(int maxFrameSize)
    {
        checkArgument(maxFrameSize <= 0x3FFFFFFF);
        this.maxFrameSize = maxFrameSize;
        return this;
    }
}
