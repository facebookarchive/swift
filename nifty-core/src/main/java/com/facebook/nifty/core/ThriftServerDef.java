/*
 * Copyright (C) 2012-2016 Facebook, Inc.
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
import com.facebook.nifty.ssl.TransportAttachObserver;
import com.facebook.nifty.ssl.SslServerConfiguration;
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
    private final Duration taskTimeout;
    private final Duration queueTimeout;

    private final ThriftFrameCodecFactory thriftFrameCodecFactory;
    private final Executor executor;
    private final String name;
    private final NiftySecurityFactory securityFactory;
    private final SslServerConfiguration sslConfiguration;
    private final TransportAttachObserver transportAttachObserver;

    public ThriftServerDef(
            String name,
            int serverPort,
            int maxFrameSize,
            int queuedResponseLimit,
            int maxConnections,
            NiftyProcessorFactory processorFactory,
            TDuplexProtocolFactory duplexProtocolFactory,
            Duration clientIdleTimeout,
            Duration taskTimeout,
            Duration queueTimeout,
            ThriftFrameCodecFactory thriftFrameCodecFactory,
            Executor executor,
            NiftySecurityFactory securityFactory,
            SslServerConfiguration sslConfiguration,
            TransportAttachObserver transportAttachObserver)
    {
        this.name = name;
        this.serverPort = serverPort;
        this.maxFrameSize = maxFrameSize;
        this.maxConnections = maxConnections;
        this.queuedResponseLimit = queuedResponseLimit;
        this.processorFactory = processorFactory;
        this.duplexProtocolFactory = duplexProtocolFactory;
        this.clientIdleTimeout = clientIdleTimeout;
        this.taskTimeout = taskTimeout;
        this.queueTimeout = queueTimeout;
        this.thriftFrameCodecFactory = thriftFrameCodecFactory;
        this.executor = executor;
        this.securityFactory = securityFactory;
        this.sslConfiguration = sslConfiguration;
        this.transportAttachObserver = transportAttachObserver;
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

    public Duration getTaskTimeout() { return taskTimeout; }

    public Duration getQueueTimeout() { return queueTimeout; }

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

    public SslServerConfiguration getSslConfiguration() { return sslConfiguration; }

    public TransportAttachObserver getTransportAttachObserver() { return transportAttachObserver; }
}
