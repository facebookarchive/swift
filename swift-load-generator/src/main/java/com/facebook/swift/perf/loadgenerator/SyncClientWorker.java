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

import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.inject.Inject;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ExecutionException;

public class SyncClientWorker extends AbstractClientWorker
{
    private static final Logger logger = LoggerFactory.getLogger(SyncClientWorker.class);
    private volatile boolean shutdownRequested = false;
    private NiftyClientChannel.Factory<? extends NiftyClientChannel> channelFactory;

    @Override
    public void shutdown()
    {
        this.shutdownRequested = true;
    }

    @ThriftService(value = "SyncLoadTest")
    public static interface LoadTest extends Closeable
    {
        @ThriftMethod
        public void noop()
                throws TException;

        public void close();
    }

    @Inject
    public SyncClientWorker(
            LoadGeneratorCommandLineConfig config,
            ThriftClientManager clientManager,
            NiftyClientChannel.Factory<? extends NiftyClientChannel> channelFactory)
    {
        super(clientManager, config);
        this.channelFactory = channelFactory;
    }

    @Override
    public void run()
    {
        // Run each synchronous client on its own thread
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (!shutdownRequested) {
                    try {
                        try (LoadTest client = clientManager.createClient(serverHostAndPort, LoadTest.class, channelFactory).get()) {
                            logger.info("Worker connected");
                            for (int i = 0; i < getOperationsPerConnection(); i++) {
                                sendRequest(client);
                            }
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }
            }
        }).start();
    }

    private void sendRequest(LoadTest client)
            throws TException, ExecutionException, InterruptedException
    {
        try {
            client.noop();
        }
        catch (TException ex) {
            requestsFailed.incrementAndGet();
            return;
        }

        requestsProcessed.incrementAndGet();
    }
}
