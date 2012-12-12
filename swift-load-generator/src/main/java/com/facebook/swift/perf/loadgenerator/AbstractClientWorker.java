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
package com.facebook.swift.perf.loadgenerator;

import java.util.concurrent.atomic.AtomicLong;

import com.facebook.swift.service.ThriftClientManager;
import com.google.common.net.HostAndPort;

public abstract class AbstractClientWorker implements Runnable
{
    protected final LoadGeneratorCommandLineConfig config;
    protected final ThriftClientManager clientManager;
    protected final HostAndPort serverHostAndPort;
    protected AtomicLong requestsProcessed = new AtomicLong(0);
    protected AtomicLong requestsFailed = new AtomicLong(0);
    protected AtomicLong requestsPending = new AtomicLong(0);

    public AbstractClientWorker(ThriftClientManager clientManager, LoadGeneratorCommandLineConfig config)
    {
        this.clientManager = clientManager;
        this.config = config;
        this.serverHostAndPort = HostAndPort.fromParts(config.serverAddress, config.serverPort);
    }

    public long collectSuccessfulOperationCount()
    {
        return requestsProcessed.getAndSet(0);
    }

    public long collectFailedOperationCount()
    {
        return requestsFailed.getAndSet(0);
    }

    public long getOperationsPerConnection()
    {
        return config.operationsPerConnection;
    }

    public void bump() {

    }

    public abstract void shutdown();
}
