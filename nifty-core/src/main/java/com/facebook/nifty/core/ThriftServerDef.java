/*
 * Copyright (C) 2012 Facebook, Inc.
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

import io.airlift.units.Duration;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TProtocolFactory;

import java.util.concurrent.Executor;

/**
 * Descriptor for a Thrift Server. This defines a listener port that Nifty need to start a Thrift endpoint.
 */
public class ThriftServerDef
{
    private final int serverPort;
    private final int maxFrameSize;
    private final int queuedResponseLimit;
    private final TProcessorFactory processorFactory;
    private final TProtocolFactory inProtocolFact;
    private final TProtocolFactory outProtocolFact;

    private final Duration clientIdleTimeout;

    private final boolean headerTransport;
    private final Executor executor;
    private final String name;
    public ThriftServerDef(
            String name,
            int serverPort,
            int maxFrameSize,
            int queuedResponseLimit,
            TProcessorFactory factory,
            TProtocolFactory inProtocolFact,
            TProtocolFactory outProtocolFact,
            Duration clientIdleTimeout,
            boolean useHeaderTransport,
            Executor executor)
    {
        this.name = name;
        this.serverPort = serverPort;
        this.maxFrameSize = maxFrameSize;
        this.queuedResponseLimit = queuedResponseLimit;
        this.processorFactory = factory;
        this.inProtocolFact = inProtocolFact;
        this.outProtocolFact = outProtocolFact;
        this.clientIdleTimeout = clientIdleTimeout;
        this.headerTransport = useHeaderTransport;
        this.executor = executor;
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

    public int getQueuedResponseLimit()
    {
        return queuedResponseLimit;
    }

    public TProcessorFactory getProcessorFactory()
    {
        return processorFactory;
    }

    public TProtocolFactory getInProtocolFactory()
    {
        return inProtocolFact;
    }

    public TProtocolFactory getOutProtocolFactory()
    {
        return outProtocolFact;
    }

    public Duration getClientIdleTimeout() {
        return clientIdleTimeout;
    }

    public boolean isHeaderTransport()
    {
        return headerTransport;
    }

    public Executor getExecutor()
    {
        return executor;
    }

    public String getName()
    {
        return name;
    }
}
