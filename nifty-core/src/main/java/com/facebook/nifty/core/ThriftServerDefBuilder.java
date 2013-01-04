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
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builder for the Thrift Server descriptor. Example :
 * <code>
 * new ThriftServerDefBuilder()
 * .listen(config.getServerPort())
 * .limitFrameSizeTo(config.getMaxFrameSize())
 * .withProcessor(new FacebookService.Processor(new MyFacebookBase()))
 * .using(Executors.newFixedThreadPool(5))
 * .build();
 * <p/>
 * <p/>
 * You can then pass ThriftServerDef to guice via a multibinder.
 * <p/>
 * </code>
 */
public class ThriftServerDefBuilder
{
    private static final AtomicInteger ID = new AtomicInteger(1);
    private int serverPort;
    private int maxFrameSize;
    private int queuedResponseLimit;
    private TProcessorFactory processorFactory;
    private TProtocolFactory inProtocolFact;
    private TProtocolFactory outProtocolFact;
    private Executor executor;
    private String name = "nifty-" + ID.getAndIncrement();
    private boolean useHeaderTransport;
    private Duration clientIdleTimeout;

    /**
     * Create a ThriftServerDefBuilder with common defaults
     */
    public ThriftServerDefBuilder()
    {
        this.serverPort = 8080;
        this.maxFrameSize = 1048576;
        this.queuedResponseLimit = 16;
        this.inProtocolFact = new TBinaryProtocol.Factory();
        this.outProtocolFact = new TBinaryProtocol.Factory();
        this.executor = new Executor()
        {
            @Override
            public void execute(Runnable runnable)
            {
                runnable.run();
            }
        };
        this.useHeaderTransport = false;
        this.clientIdleTimeout = null;
    }

    /**
     * Give the endpoint a more meaningful name.
     */
    public ThriftServerDefBuilder name(String name)
    {
        this.name = name;
        return this;
    }

    /**
     * Listen to this port.
     */
    public ThriftServerDefBuilder listen(int serverPort)
    {
        this.serverPort = serverPort;
        return this;
    }

    /**
     * Specify protocolFactory for both input and output
     */
    public ThriftServerDefBuilder speaks(TProtocolFactory tProtocolFactory)
    {
        this.inProtocolFact = tProtocolFactory;
        this.outProtocolFact = tProtocolFactory;
        return this;
    }

    /**
     * Specify the TProcessor.
     */
    public ThriftServerDefBuilder withProcessor(TProcessor p)
    {
        this.processorFactory = new TProcessorFactory(p);
        return this;
    }

    /**
     * Set frame size limit.  Default is 1M
     */
    public ThriftServerDefBuilder limitFrameSizeTo(int maxFrameSize)
    {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    /**
     * Limit number of queued responses per connection, before pausing reads
     * to catch up.
     */
    public ThriftServerDefBuilder limitQueuedResponsesPerConnection(int queuedResponseLimit)
    {
        this.queuedResponseLimit = queuedResponseLimit;
        return this;
    }

    /**
     * Anohter way to specify the TProcessor.
     */
    public ThriftServerDefBuilder withProcessorFactory(TProcessorFactory processorFactory)
    {
        this.processorFactory = processorFactory;
        return this;
    }

    /**
     * Specify only the input protocol.
     */
    public ThriftServerDefBuilder inProtocol(TProtocolFactory tProtocolFactory)
    {
        this.inProtocolFact = tProtocolFactory;
        return this;
    }

    /**
     * Specify only the output protocol.
     */
    public ThriftServerDefBuilder outProtocol(TProtocolFactory tProtocolFactory)
    {
        this.outProtocolFact = tProtocolFactory;
        return this;
    }

    /**
     * Specify timeout during which if connected client doesn't send a message, server
     * will disconnect the client
     */
    public ThriftServerDefBuilder clientIdleTimeout(Duration clientIdleTimeout)
    {
        this.clientIdleTimeout = clientIdleTimeout;
        return this;
    }

    /**
     * Specify an executor for thrift processor invocations ( i.e. = THaHsServer )
     * By default invocation happens in Netty single thread
     * ( i.e. = TNonBlockingServer )
     */
    public ThriftServerDefBuilder using(Executor exe)
    {
        this.executor = exe;
        return this;
    }

    public ThriftServerDefBuilder usingHeaderTransport()
    {
        this.useHeaderTransport = true;
        return this;
    }

    /**
     * Build the ThriftServerDef
     */
    public ThriftServerDef build()
    {
        if (processorFactory == null) {
            throw new IllegalStateException("processor not defined !");
        }
        return new ThriftServerDef(
                name,
                serverPort,
                maxFrameSize,
                queuedResponseLimit,
                processorFactory,
                inProtocolFact,
                outProtocolFact,
                clientIdleTimeout,
                useHeaderTransport,
                executor);
    }
}
