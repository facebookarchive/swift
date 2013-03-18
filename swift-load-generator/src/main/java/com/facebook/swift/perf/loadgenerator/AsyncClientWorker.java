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

import static java.lang.Math.max;

import com.facebook.nifty.client.NiftyClientConnector;
import io.airlift.units.Duration;

import java.io.Closeable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;

public final class AsyncClientWorker extends AbstractClientWorker implements FutureCallback<Object>
{
    private static final Logger logger = LoggerFactory.getLogger(AsyncClientWorker.class);
    private static final int MAX_FRAME_SIZE = 0x7FFFFFFF;

    private volatile boolean shutdownRequested = false;
    private final long pendingOperationsLowWaterMark;
    private final long pendingOperationsHighWaterMark;
    private final Executor simpleExecutor;
    private NiftyClientChannel channel;
    private NiftyClientConnector<? extends NiftyClientChannel> connector;
    private LoadTest client;

    private AtomicLong sentRequests = new AtomicLong(0);
    private long connectionRequestCutoff;

    @Override
    public void shutdown()
    {
        this.shutdownRequested = true;
    }

    @ThriftService(value = "AsyncLoadTest")
    public static interface LoadTest extends Closeable
    {
        @ThriftMethod
        public ListenableFuture<Void> noop()
                throws TException;

        public void close();
    }

    @Inject
    public AsyncClientWorker(
            LoadGeneratorCommandLineConfig config,
            ThriftClientManager clientManager,
            NiftyClientConnector<? extends NiftyClientChannel> connector)
    {
        super(clientManager, config);

        this.connector = connector;

        // Keep the pipe full with between target and target * 2 operations
        pendingOperationsLowWaterMark = max(config.targetAsyncOperationsPending * 9 / 10, 1);
        pendingOperationsHighWaterMark = max(config.targetAsyncOperationsPending * 11 / 10, 2);

        // Could have just used MoreExecutors.sameThreadExecutor(), but it has some overhead
        // associated with implementing a full ExecutorService.
        simpleExecutor = new Executor()
        {
            @Override
            public void execute(Runnable command)
            {
                command.run();
            }
        };
    }

    @Override
    public void run()
    {
        try {
            ListenableFuture<LoadTest> clientFuture;

            if (client != null) {
                throw new IllegalStateException("Each worker should create only client at a time");
            }

            connectionRequestCutoff = requestsProcessed.get() + getOperationsPerConnection();

            clientFuture = clientManager.createClient(connector,
                                                      LoadTest.class,
                                                      new Duration(config.connectTimeoutMilliseconds, TimeUnit.SECONDS),
                                                      new Duration(config.sendTimeoutMilliseconds, TimeUnit.MILLISECONDS),
                                                      new Duration(config.receiveTimeoutMilliseconds, TimeUnit.MILLISECONDS),
                                                      MAX_FRAME_SIZE,
                                                      "AsyncClientWorker",
                                                      null);

            Futures.addCallback(clientFuture, new FutureCallback<LoadTest>()
            {
                @Override
                public void onSuccess(LoadTest result)
                {
                    logger.trace("Worker connected");

                    client = result;
                    channel = clientManager.getNiftyChannel(client);

                    // Thrift clients are not thread-safe, and for maximum efficiency, new requests are made
                    // on the channel thread, as the pipeline starts to clear out. So we either need to
                    // synchronize on "sendRequest" or make the initial calls to fill the pipeline on the
                    // channel thread as well.
                    channel.executeInIoThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            fillRequestPipeline(client);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t)
                {
                    onConnectFailed(t);
                }
            });
        }
        catch (Throwable t) {
            onConnectFailed(t);
        }
    }

    private void onConnectFailed(Throwable cause)
    {
        logger.error("Could not connect: " + cause.getMessage());

        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    public void reconnect()
    {
        if (client != null) {
            client.close();
            client = null;
        }
        run();
    }

    protected long sendRequest(final LoadTest client)
            throws TException
    {
        long pending = requestsPending.incrementAndGet();
        final ListenableFuture<Void> future = client.noop();
        Futures.addCallback(future, this, simpleExecutor);
        return pending;
    }

    protected void fillRequestPipeline(final LoadTest client)
    {
        try {
            while (!shutdownRequested) {
                if (channel.hasError()) {
                    throw channel.getError();
                }

                if (sendRequest(client) >= pendingOperationsHighWaterMark) {
                    break;
                }

                if (sentRequests.incrementAndGet() >= connectionRequestCutoff) {
                    break;
                }
            }
        }
        catch (TException ex) {
            // sending a request failed
            logger.error("Async client request failed: {}",
                         Throwables.getRootCause(ex).getMessage());
            client.close();
        }
    }

    @Override
    public void onSuccess(@Nullable Object result)
    {
        requestsProcessed.incrementAndGet();
        if ((requestsPending.decrementAndGet() < pendingOperationsLowWaterMark) &&
            (sentRequests.get() <= connectionRequestCutoff)) {
            fillRequestPipeline(client);
        }
    }

    @Override
    public void onFailure(Throwable t)
    {
        if (t instanceof TException) {
            client.close();
            logger.error("Async client received failure response: {}",
                         Throwables.getRootCause(t).getMessage());
        }

        requestsFailed.incrementAndGet();
        requestsPending.decrementAndGet();
    }
}
