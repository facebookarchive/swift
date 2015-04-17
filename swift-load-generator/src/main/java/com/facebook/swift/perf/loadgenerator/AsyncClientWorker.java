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
import com.facebook.swift.service.ThriftClient;
import com.facebook.swift.service.ThriftClientManager;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import io.airlift.log.Logger;
import org.apache.thrift.TException;

import javax.annotation.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.max;

public final class AsyncClientWorker extends AbstractClientWorker
{
    private static final AtomicInteger clientCounter = new AtomicInteger(0);
    private static final Logger logger = Logger.get(AsyncClientWorker.class);
    private static final int MAX_FRAME_SIZE = 0x7FFFFFFF;
    private final ThriftClient<AsyncLoadTest> client;
    private final ThriftClientManager clientManager;

    private volatile boolean shutdownRequested = false;
    private final long pendingOperationsLowWaterMark;
    private final long pendingOperationsHighWaterMark;
    private final Executor simpleExecutor;
    private NiftyClientConnector<? extends NiftyClientChannel> connector;
    private ClientWrapper clientWrapper;

    @Override
    public void shutdown()
    {
        this.shutdownRequested = true;
    }

    @Inject
    public AsyncClientWorker(
            LoadGeneratorCommandLineConfig config,
            ThriftClientManager clientManager,
            ThriftClient<AsyncLoadTest> client,
            NiftyClientConnector<? extends NiftyClientChannel> connector)
    {
        super(config);

        this.connector = connector;
        this.clientManager = clientManager;
        this.client = client;

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
            ListenableFuture<AsyncLoadTest> clientFuture;

            clientFuture = client.open(connector);

            ListenableFuture<ClientWrapper> wrapperFuture = Futures.transform(clientFuture, new Function<AsyncLoadTest, ClientWrapper>()
            {
                @Nullable
                @Override
                public ClientWrapper apply(@Nullable AsyncLoadTest client)
                {
                    return new ClientWrapper(clientManager, client, config.operationsPerConnection);
                }
            });

            Futures.addCallback(wrapperFuture, new FutureCallback<ClientWrapper>()
            {
                @Override
                public void onSuccess(ClientWrapper result)
                {
                    logger.debug("Worker connected");

                    clientWrapper = result;
                    NiftyClientChannel channel = clientManager.getNiftyChannel(clientWrapper.getClient());

                    // Thrift clients are not thread-safe, and for maximum efficiency, new requests are made
                    // on the channel thread, as the pipeline starts to clear out. So we either need to
                    // synchronize on "sendRequest" or make the initial calls to fill the pipeline on the
                    // channel thread as well.
                    channel.executeInIoThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            fillRequestPipeline(clientWrapper);
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
        logger.error("Could not connect: %s", cause);
        reconnect();
    }

    @Override
    public synchronized void reconnect()
    {
        run();
    }

    protected long sendRequest(ClientWrapper clientWrapper)
            throws TException
    {
        final AsyncLoadTest client = clientWrapper.getClient();
        ListenableFuture<?> future;
        Operation operation = nextOperation();
        switch (operation) {
            case NOOP:
                future = client.noop();
                break;
            case ONEWAY_NOOP:
                client.onewayNoop();
                future = Futures.<Void>immediateFuture(null);
                break;
            case ASYNC_NOOP:
                future = client.asyncNoop();
                break;
            case ADD:
                future = client.add(1,2);
                break;
            case ECHO:
                future = client.echo(getNextSendBuffer());
                break;
            case SEND:
                future = client.send(getNextSendBuffer());
                break;
            case RECV:
                future = client.recv(getNextReceiveBufferSize());
                break;
            case SEND_RECV:
                future = client.sendrecv(getNextSendBuffer(), getNextReceiveBufferSize());
                break;
            case ONEWAY_SEND:
                client.onewaySend(getNextSendBuffer());
                future = Futures.<Void>immediateFuture(null);
                break;
            case ONEWAY_THROW:
                client.onewayThrow(getNextExceptionCode());
                future = Futures.<Void>immediateFuture(null);
                break;
            case THROW_UNEXPECTED:
                future = client.throwUnexpected(getNextExceptionCode());
                break;
            case THROW_ERROR:
                future = client.throwError(getNextSendBufferSize());
                break;
            case SLEEP:
                future = client.sleep(getNextSleepMicroseconds());
                break;
            case ONEWAY_SLEEP:
                client.onewaySleep(getNextSleepMicroseconds());
                future = Futures.<Void>immediateFuture(null);
                break;
            case BAD_BURN:
                future = client.badBurn(getNextBurnMicroseconds());
                break;
            case BAD_SLEEP:
                future = client.badSleep(getNextSleepMicroseconds());
                break;
            case ONEWAY_BURN:
                client.onewayBurn(getNextBurnMicroseconds());
                future = Futures.<Void>immediateFuture(null);
                break;
            case BURN:
                future = client.burn(getNextBurnMicroseconds());
                break;
            default:
                throw new IllegalStateException("Unknown operation type");
        }

        long pending = requestsPending.incrementAndGet();
        Futures.addCallback(future, new RequestCallback(clientWrapper), simpleExecutor);
        return pending;
    }

    protected void fillRequestPipeline(final ClientWrapper clientWrapper) {
        // We've already finished sending requests on this client
        if (clientWrapper.shouldStopSending()) {
            return;
        }

        try {
            while (!shutdownRequested) {
                if (clientWrapper.hasError()) {
                    throw clientWrapper.getError();
                }

                long pendingCount = sendRequest(clientWrapper);

                clientWrapper.recordRequestSent();
                if (clientWrapper.shouldStopSending()) {
                    reconnect();
                    break;
                }

                if (pendingCount >= pendingOperationsHighWaterMark) {
                    break;
                }
            }
        }
        catch (TException ex) {
            // sending a request failed
            logger.error("Async client request failed: %s",
                         Throwables.getRootCause(ex).getMessage());
            clientWrapper.close();
        }
    }

    private class ClientWrapper {
        private final AtomicLong requestsSent = new AtomicLong(0);
        private final AtomicLong responsesReceived = new AtomicLong(0);
        private final long requestLimit;
        private final int clientId;
        private final ThriftClientManager clientManager;
        private AsyncLoadTest client;

        public ClientWrapper(ThriftClientManager clientManager, AsyncLoadTest client, long requestLimit)
        {
            this.clientManager = clientManager;
            this.client = client;
            this.requestLimit = requestLimit;
            this.clientId = clientCounter.getAndIncrement();
        }

        public AsyncLoadTest getClient()
        {
            return client;
        }

        public NiftyClientChannel getChannel()
        {
            return clientManager.getNiftyChannel(getClient());
        }

        public TException getError()
        {
            return getChannel().getError();
        }

        public boolean hasError()
        {
            return getError() != null;
        }

        public void close()
        {
            getClient().close();
        }

        public long recordRequestSent()
        {
            return requestsSent.incrementAndGet();
        }

        public long recordResponseReceived()
        {
            return responsesReceived.incrementAndGet();
        }

        public boolean shouldStopSending()
        {
            return requestsSent.get() >= requestLimit;
        }

        public boolean isFinishedReceivingResponses()
        {
            return responsesReceived.get() >= requestLimit;
        }

        public int getClientId()
        {
            return clientId;
        }
    }

    private class RequestCallback implements FutureCallback<Object>
    {
        private final ClientWrapper clientWrapper;

        public RequestCallback(ClientWrapper clientWrapper)
        {
            this.clientWrapper = clientWrapper;
        }

        @Override
        public void onSuccess(@Nullable Object result)
        {
            clientWrapper.recordResponseReceived();
            if (clientWrapper.isFinishedReceivingResponses())
            {
                clientWrapper.close();
            }

            requestsProcessed.incrementAndGet();

            if (requestsPending.decrementAndGet() < pendingOperationsLowWaterMark) {
                fillCurrentClientPipeline();
            }
        }

        @Override
        public void onFailure(Throwable t)
        {
            if (t instanceof LoadError) {
                onSuccess(null);
                return;
            }

            clientWrapper.recordResponseReceived();
            if (clientWrapper.isFinishedReceivingResponses())
            {
                clientWrapper.close();
            }

            if (t instanceof TException) {
                clientWrapper.close();
                logger.error("Async client received failure response: %s",
                             Throwables.getRootCause(t).getMessage());
            }

            requestsFailed.incrementAndGet();

            if (requestsPending.decrementAndGet() < pendingOperationsLowWaterMark) {
                fillCurrentClientPipeline();
            }
        }

        private void fillCurrentClientPipeline()
        {
            // Fill the pipeline using the most recently connected client controlled by this
            // worker (which is not necessarily the same client as the one that has just received
            // a success or failure callback)
            ClientWrapper currentClientWrapper = AsyncClientWorker.this.clientWrapper;
            fillRequestPipeline(currentClientWrapper);
        }
    }
}
