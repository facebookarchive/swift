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

import com.facebook.nifty.codec.ThriftFrameCodecFactory;
import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import com.facebook.nifty.processor.NiftyProcessorFactory;
import io.airlift.units.Duration;

import java.util.concurrent.Executor;

/**
 * Descriptor for a Thrift Server. This defines a listener port that Nifty need to start a Thrift endpoint.
 */
public class ThriftServerDef
{
    private final int serverPort;
    private final int maxFrameSize;
    private final int maxConnections;
    private final int queuedResponseLimit;
    private final NiftyProcessorFactory processorFactory;
    private final TDuplexProtocolFactory duplexProtocolFactory;

    private final Duration clientIdleTimeout;

    private final ThriftFrameCodecFactory thriftFrameCodecFactory;
    private final Executor executor;
    private final String name;
    private final NiftySecurityFactory securityFactory;

    public ThriftServerDef(
            String name,
            int serverPort,
            int maxFrameSize,
            int queuedResponseLimit,
            int maxConnections,
            NiftyProcessorFactory processorFactory,
            TDuplexProtocolFactory duplexProtocolFactory,
            Duration clientIdleTimeout,
            ThriftFrameCodecFactory thriftFrameCodecFactory,
            Executor executor,
            NiftySecurityFactory securityFactory)
    {
        this.name = name;
        this.serverPort = serverPort;
        this.maxFrameSize = maxFrameSize;
        this.maxConnections = maxConnections;
        this.queuedResponseLimit = queuedResponseLimit;
        this.processorFactory = processorFactory;
        this.duplexProtocolFactory = duplexProtocolFactory;
        this.clientIdleTimeout = clientIdleTimeout;
        this.thriftFrameCodecFactory = thriftFrameCodecFactory;
        this.executor = executor;
        this.securityFactory = securityFactory;
    }

    public static ThriftServerDefBuilder newBuilder()
    {
        return new ThriftServerDefBuilder();
    }

    public int getServerPort()
    {
        return serverPort;
    }

    public int getMaxFrameSize()
    {
        return maxFrameSize;
    }

    public int getMaxConnections()
    {
        return maxConnections;
    }

    public int getQueuedResponseLimit()
    {
        return queuedResponseLimit;
    }

    public NiftyProcessorFactory getProcessorFactory()
    {
        return processorFactory;
    }

    public TDuplexProtocolFactory getDuplexProtocolFactory()
    {
        return duplexProtocolFactory;
    }

    public Duration getClientIdleTimeout() {
        return clientIdleTimeout;
    }

    public Executor getExecutor()
    {
        return executor;
    }

    public String getName()
    {
        return name;
    }

    public ThriftFrameCodecFactory getThriftFrameCodecFactory()
    {
        return thriftFrameCodecFactory;
    }

    public NiftySecurityFactory getSecurityFactory()
    {
        return securityFactory;
    }
}
