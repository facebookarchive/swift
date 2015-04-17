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
package com.facebook.swift.perf.loadgenerator;

import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.nifty.client.NiftyClientConnector;
import com.facebook.swift.service.RuntimeTException;
import com.facebook.swift.service.ThriftClient;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Inject;
import io.airlift.log.Logger;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SyncClientWorker extends AbstractClientWorker
{
    private static final Logger logger = Logger.get(SyncClientWorker.class);
    private final ThriftClient<SyncLoadTest> client;
    private volatile boolean shutdownRequested = false;
    private NiftyClientConnector<? extends NiftyClientChannel> connector;

    @Override
    public void shutdown()
    {
        this.shutdownRequested = true;
    }

    @Inject
    public SyncClientWorker(
            LoadGeneratorCommandLineConfig config,
            ThriftClient<SyncLoadTest> client,
            NiftyClientConnector<? extends NiftyClientChannel> connector)
    {
        super(config);
        this.connector = connector;
        this.client = client;
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
                        try (SyncLoadTest client = SyncClientWorker.this.client.open(connector).get()) {
                            logger.debug("Worker connected");
                            for (int i = 0; i < getOperationsPerConnection(); i++) {
                                sendRequest(client);
                            }
                        }
                    }
                    catch (ExecutionException | TException ex) {
                        logger.error("Connection failed: %s", ex.getMessage());
                        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
                    }
                    catch (InterruptedException ex) {
                        logger.error("Connection interrupted");
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
    }

    private void sendRequest(SyncLoadTest client)
            throws TException
    {
        try {
            Operation operation = nextOperation();
            switch (operation) {
                case NOOP:
                    client.noop();
                    break;
                case ONEWAY_NOOP:
                    client.onewayNoop();
                    break;
                case ASYNC_NOOP:
                    client.asyncNoop();
                    break;
                case ADD:
                    long addend1 = getNextAddOperand();
                    long addend2 = getNextAddOperand();
                    long result = client.add(addend1, addend2);
                    if (result != addend1 + addend2) {
                        logger.error("Server returned incorrect addition result");
                    }
                    break;
                case ECHO:
                    client.echo(getNextSendBuffer());
                    break;
                case SEND:
                    client.send(getNextSendBuffer());
                    break;
                case RECV:
                    client.recv(getNextReceiveBufferSize());
                    break;
                case SEND_RECV:
                    client.sendrecv(getNextSendBuffer(), getNextReceiveBufferSize());
                    break;
                case ONEWAY_SEND:
                    client.onewaySend(getNextSendBuffer());
                    break;
                case ONEWAY_THROW:
                    client.onewayThrow(getNextExceptionCode());
                    break;
                case THROW_UNEXPECTED:
                    try {
                        client.throwUnexpected(getNextExceptionCode());
                    }
                    catch (TApplicationException e) {
                        // Exception is expected, don't need to do anything here
                        break;
                    }
                    break;
                case THROW_ERROR:
                    try {
                        client.throwError(getNextSendBufferSize());
                    }
                    catch (LoadError e) {
                        // Exception is expected, don't need to do anything here
                        break;
                    }
                    break;
                case SLEEP:
                    client.sleep(getNextSleepMicroseconds());
                    break;
                case ONEWAY_SLEEP:
                    client.onewaySleep(getNextSleepMicroseconds());
                    break;
                case BAD_BURN:
                    client.badBurn(getNextBurnMicroseconds());
                    break;
                case BAD_SLEEP:
                    client.badSleep(getNextSleepMicroseconds());
                    break;
                case ONEWAY_BURN:
                    client.onewayBurn(getNextBurnMicroseconds());
                    break;
                case BURN:
                    client.burn(getNextBurnMicroseconds());
                    break;
                default:
                    throw new IllegalStateException("Unknown operation type");
            }
        } catch (RuntimeTException ex) {
            logger.error(ex.getMessage());
            requestsFailed.incrementAndGet();
            throw ex;
        }

        requestsProcessed.incrementAndGet();
    }
}
