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

import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import com.facebook.nifty.duplex.TProtocolPair;
import com.facebook.nifty.duplex.TTransportPair;
import com.facebook.nifty.processor.NiftyProcessorFactory;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.util.Timer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;

import static com.google.common.base.Preconditions.checkState;

/**
 * Dispatch TNiftyTransport to the TProcessor and write output back.
 *
 * Note that all current async thrift clients are capable of sending multiple requests at once
 * but not capable of handling out-of-order responses to those requests, so this dispatcher
 * sends the requests in order. (Eventually this will be conditional on a flag in the thrift
 * message header for future async clients that can handle out-of-order responses).
 */
public class NiftyDispatcher extends SimpleChannelUpstreamHandler
{
    private final NiftyProcessorFactory processorFactory;
    private final Executor exe;
    private final long taskTimeoutMillis;
    private final Timer taskTimeoutTimer;
    private final int queuedResponseLimit;
    private final Map<Integer, ThriftMessage> responseMap = new HashMap<>();
    private final AtomicInteger dispatcherSequenceId = new AtomicInteger(0);
    private final AtomicInteger lastResponseWrittenId = new AtomicInteger(0);
    private final TDuplexProtocolFactory duplexProtocolFactory;

    public NiftyDispatcher(ThriftServerDef def, Timer timer)
    {
        this.processorFactory = def.getProcessorFactory();
        this.duplexProtocolFactory = def.getDuplexProtocolFactory();
        this.queuedResponseLimit = def.getQueuedResponseLimit();
        this.exe = def.getExecutor();
        this.taskTimeoutMillis = (def.getTaskTimeout() == null ? 0 : def.getTaskTimeout().toMillis());
        this.taskTimeoutTimer = (def.getTaskTimeout() == null ? null : timer);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception
    {
        if (e.getMessage() instanceof ThriftMessage) {
            ThriftMessage message = (ThriftMessage) e.getMessage();
            if (taskTimeoutMillis > 0) {
                message.setProcessStartTimeMillis(System.currentTimeMillis());
            }
            checkResponseOrderingRequirements(ctx, message);

            DispatcherContext.setupTransportBuffersForRequest(ctx, message, duplexProtocolFactory);

            try {
                processRequest(ctx, message);
            }
            catch (RejectedExecutionException ex) {
                TApplicationException x = new TApplicationException(TApplicationException.INTERNAL_ERROR,
                        "Server overloaded");
                sendTApplicationException(x, ctx, message);
            }
        }
        else {
            ctx.sendUpstream(e);
        }
    }

    private void checkResponseOrderingRequirements(ChannelHandlerContext ctx, ThriftMessage message)
    {
        boolean messageRequiresOrderedResponses = message.isOrderedResponsesRequired();

        if (!DispatcherContext.isResponseOrderingRequirementInitialized(ctx)) {
            // This is the first request. This message will decide whether all responses on the
            // channel must be strictly ordered, or whether out-of-order is allowed.
            DispatcherContext.setResponseOrderingRequired(ctx, messageRequiresOrderedResponses);
        }
        else {
            // This is not the first request. Verify that the ordering requirement on this message
            // is consistent with the requirement on the channel itself.
            checkState(
                    messageRequiresOrderedResponses == DispatcherContext.isResponseOrderingRequired(ctx),
                    "Every message on a single channel must specify the same requirement for response ordering");
        }
    }

    private void processRequest(
            final ChannelHandlerContext ctx,
            final ThriftMessage message) {
        // Remember the ordering of requests as they arrive, used to enforce an order on the
        // responses.
        final int requestSequenceId = dispatcherSequenceId.incrementAndGet();

        synchronized (responseMap)
        {
            // Limit the number of pending responses (responses which finished out of order, and are
            // waiting for previous requests to be finished so they can be written in order), by
            // blocking further channel reads. Due to the way Netty frame decoders work, this is more
            // of an estimate than a hard limit. Netty may continue to decode and process several
            // more requests that were in the latest read, even while further reads on the channel
            // have been blocked.
            if (requestSequenceId > lastResponseWrittenId.get() + queuedResponseLimit &&
                !DispatcherContext.isChannelReadBlocked(ctx))
            {
                DispatcherContext.blockChannelReads(ctx);
            }
        }

        exe.execute(new Runnable()
        {
            @Override
            public void run()
            {
                ListenableFuture<Boolean> processFuture;
                final AtomicBoolean responseSent = new AtomicBoolean(false);

                final TTransportPair transportPair = DispatcherContext.getTransportPair(ctx);
                TProtocolPair protocolPair = DispatcherContext.getProtocolPair(ctx);

                try {
                    try {
                        long timeRemaining = 0;
                        if (taskTimeoutMillis > 0) {
                            long timeElapsed = System.currentTimeMillis() - message.getProcessStartTimeMillis();
                            if (timeElapsed >= taskTimeoutMillis) {
                                TApplicationException taskTimeoutException = new TApplicationException(
                                        TApplicationException.INTERNAL_ERROR,
                                        "Task stayed on the queue for " + timeElapsed +
                                        " milliseconds, exceeding configured task timeout of " + taskTimeoutMillis +
                                        " milliseconds.");
                                sendTApplicationException(taskTimeoutException, ctx, message);
                                return;
                            } else {
                                timeRemaining = taskTimeoutMillis - timeElapsed;
                            }
                        }

                        if (timeRemaining > 0) {
                            taskTimeoutTimer.newTimeout(new TimerTask() {
                                @Override
                                public void run(Timeout timeout) throws Exception {
                                    // The immediateFuture returned by processors isn't cancellable, cancel() and
                                    // isCanceled() always return false. Use a flag to detect task expiration.
                                    if(responseSent.compareAndSet(false, true)) {
                                        TApplicationException ex = new TApplicationException(
                                                TApplicationException.INTERNAL_ERROR,
                                                "Task timed out while executing."
                                        );
                                        sendTApplicationException(ex, ctx, message);
                                    }
                                }
                            }, timeRemaining, TimeUnit.MILLISECONDS);
                        }

                        ConnectionContext connectionContext = ConnectionContexts.getContext(ctx.getChannel());
                        RequestContext requestContext = new NiftyRequestContext(connectionContext, transportPair, protocolPair);
                        RequestContexts.setCurrentContext(requestContext);
                        TTransport compositeTransport = new TInputOutputCompositeTransport(transportPair.getInputTransport(), transportPair.getOutputTransport());
                        processFuture = processorFactory.getProcessor(compositeTransport).process(protocolPair.getInputProtocol(), protocolPair.getOutputProtocol(), requestContext);
                    }
                    finally {
                        // RequestContext does NOT stay set while we are waiting for the process
                        // future to complete. This is by design because we'll might move on to the
                        // next request using this thread before this one is completed. If you need
                        // the context throughout an asynchronous handler, you need to read and store
                        // it before returning a future.
                        RequestContexts.clearCurrentContext();
                    }

                    Futures.addCallback(
                            processFuture,
                            new FutureCallback<Boolean>()
                            {
                                @Override
                                public void onSuccess(Boolean result)
                                {
                                    try {
                                        // Only write response if the client is still there and the task timeout
                                        // hasn't expired.
                                        if (ctx.getChannel().isConnected() && responseSent.compareAndSet(false, true)) {
                                            TChannelBufferOutputTransport outputTransport =
                                                    (TChannelBufferOutputTransport) transportPair.getOutputTransport();
                                            ThriftMessage response = message.getMessageFactory().create(
                                                    outputTransport.getOutputBuffer());
                                            writeResponse(ctx, response, requestSequenceId,
                                                    DispatcherContext.isResponseOrderingRequired(ctx));
                                        }
                                    }
                                    catch (Throwable t) {
                                        onDispatchException(ctx, t);
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t)
                                {
                                    onDispatchException(ctx, t);
                                }
                            });
                }
                catch (TException e) {
                    onDispatchException(ctx, e);
                }
            }
        });
    }

    private void sendTApplicationException(
            TApplicationException x,
            ChannelHandlerContext ctx,
            ThriftMessage request)
    {
        if (ctx.getChannel().isConnected()) {
            try {
                ChannelBuffer duplicatedRequestBuffer = request.getBuffer().duplicate();
                duplicatedRequestBuffer.resetReaderIndex();

                TTransportPair transportPair = DispatcherContext.getTransportPair(ctx);
                TProtocolPair protocolPair = DispatcherContext.getProtocolPair(ctx);

                TChannelBufferInputTransport inputTransport = (TChannelBufferInputTransport) transportPair.getInputTransport();
                TChannelBufferOutputTransport outputTransport = (TChannelBufferOutputTransport) transportPair.getOutputTransport();
                inputTransport.setInputBuffer(duplicatedRequestBuffer);

                TProtocol outputProtocol = protocolPair.getOutputProtocol();
                TProtocol inputProtocol = protocolPair.getInputProtocol();

                TMessage message = inputProtocol.readMessageBegin();
                outputProtocol.writeMessageBegin(new TMessage(message.name, TMessageType.EXCEPTION, message.seqid));
                x.write(outputProtocol);
                outputProtocol.writeMessageEnd();
                outputProtocol.getTransport().flush();

                ThriftMessage response = request.getMessageFactory().create(outputTransport.getOutputBuffer());
                writeResponse(ctx, response, message.seqid, DispatcherContext.isResponseOrderingRequired(ctx));
            }
            catch (TException ex) {
                onDispatchException(ctx, ex);
            }
        }
    }

    private void onDispatchException(ChannelHandlerContext ctx, Throwable t)
    {
        Channels.fireExceptionCaught(ctx, t);
        closeChannel(ctx);
    }

    private void writeResponse(ChannelHandlerContext ctx,
                               ThriftMessage response,
                               int responseSequenceId,
                               boolean isOrderedResponsesRequired)
    {
        if (isOrderedResponsesRequired) {
            writeResponseInOrder(ctx, response, responseSequenceId);
        }
        else {
            // No ordering required, just write the response immediately
            Channels.write(ctx.getChannel(), response);
            lastResponseWrittenId.incrementAndGet();
        }
    }

    private void writeResponseInOrder(ChannelHandlerContext ctx,
                                      ThriftMessage response,
                                      int responseSequenceId)
    {
        // Ensure responses to requests are written in the same order the requests
        // were received.
        synchronized (responseMap) {
            int currentResponseId = lastResponseWrittenId.get() + 1;
            if (responseSequenceId != currentResponseId) {
                // This response is NOT next in line of ordered responses, save it to
                // be sent later, after responses to all earlier requests have been
                // sent.
                responseMap.put(responseSequenceId, response);
            } else {
                // This response was next in line, write this response now, and see if
                // there are others next in line that should be sent now as well.
                do {
                    Channels.write(ctx.getChannel(), response);
                    lastResponseWrittenId.incrementAndGet();
                    ++currentResponseId;
                    response = responseMap.remove(currentResponseId);
                } while (null != response);

                // Now that we've written some responses, check if reads should be unblocked
                if (DispatcherContext.isChannelReadBlocked(ctx)) {
                    int lastRequestSequenceId = dispatcherSequenceId.get();
                    if (lastRequestSequenceId <= lastResponseWrittenId.get() + queuedResponseLimit) {
                        DispatcherContext.unblockChannelReads(ctx);
                    }
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception
    {
        // Any out of band exception are caught here and we tear down the socket
        closeChannel(ctx);

        // Send for logging
        ctx.sendUpstream(e);
    }

    private void closeChannel(ChannelHandlerContext ctx)
    {
        if (ctx.getChannel().isOpen()) {
            ctx.getChannel().close();
        }
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        // Reads always start out unblocked
        DispatcherContext.unblockChannelReads(ctx);
        super.channelOpen(ctx, e);
    }

    private static class DispatcherContext
    {
        private TTransportPair transportPair;
        private TProtocolPair protocolPair;
        private ReadBlockedState readBlockedState = ReadBlockedState.NOT_BLOCKED;
        private boolean responseOrderingRequired = false;
        private boolean responseOrderingRequirementInitialized = false;

        public static boolean isChannelReadBlocked(ChannelHandlerContext ctx) {
            return getDispatcherContext(ctx).readBlockedState == ReadBlockedState.BLOCKED;
        }

        public static void blockChannelReads(ChannelHandlerContext ctx) {
            // Remember that reads are blocked (there is no Channel.getReadable())
            getDispatcherContext(ctx).readBlockedState = ReadBlockedState.BLOCKED;

            // NOTE: this shuts down reads, but isn't a 100% guarantee we won't get any more messages.
            // It sets up the channel so that the polling loop will not report any new read events
            // and netty won't read any more data from the socket, but any messages already fully read
            // from the socket before this ran may still be decoded and arrive at this handler. Thus
            // the limit on queued messages before we block reads is more of a guidance than a hard
            // limit.
            ctx.getChannel().setReadable(false);
        }

        public static void unblockChannelReads(ChannelHandlerContext ctx) {
            // Remember that reads are unblocked (there is no Channel.getReadable())
            getDispatcherContext(ctx).readBlockedState = ReadBlockedState.NOT_BLOCKED;
            ctx.getChannel().setReadable(true);
        }

        public static void setResponseOrderingRequired(ChannelHandlerContext ctx, boolean required)
        {
            DispatcherContext dispatcherContext = getDispatcherContext(ctx);
            dispatcherContext.responseOrderingRequirementInitialized = true;
            dispatcherContext.responseOrderingRequired = required;
        }

        public static boolean isResponseOrderingRequired(ChannelHandlerContext ctx)
        {
            return getDispatcherContext(ctx).responseOrderingRequired;
        }

        public static boolean isResponseOrderingRequirementInitialized(ChannelHandlerContext ctx)
        {
            return getDispatcherContext(ctx).responseOrderingRequirementInitialized;
        }

        public static void setupTransportBuffersForRequest(ChannelHandlerContext ctx, ThriftMessage message, TDuplexProtocolFactory protocolFactory)
        {
            DispatcherContext dispatcherContext = getDispatcherContext(ctx);

            if (dispatcherContext.transportPair == null) {
                TChannelBufferInputTransport inputTransport = new TChannelBufferInputTransport(message.getBuffer());
                TChannelBufferOutputTransport outputTransport = new TChannelBufferOutputTransport();

                dispatcherContext.transportPair = TTransportPair.fromSeparateTransports(inputTransport, outputTransport);
                dispatcherContext.protocolPair = protocolFactory.getProtocolPair(dispatcherContext.transportPair);
            }
            else {
                TTransportPair transportPair = dispatcherContext.transportPair;
                TChannelBufferInputTransport inputTransport = (TChannelBufferInputTransport) transportPair.getInputTransport();
                TChannelBufferOutputTransport outputTransport = (TChannelBufferOutputTransport) transportPair.getOutputTransport();

                inputTransport.setInputBuffer(message.getBuffer());
                outputTransport.resetOutputBuffer();
            }
        }

        public static TTransportPair getTransportPair(ChannelHandlerContext ctx)
        {
            return DispatcherContext.getDispatcherContext(ctx).transportPair;
        }

        public static TProtocolPair getProtocolPair(ChannelHandlerContext ctx)
        {
            return DispatcherContext.getDispatcherContext(ctx).protocolPair;
        }

        private static DispatcherContext getDispatcherContext(ChannelHandlerContext ctx)
        {
            DispatcherContext dispatcherContext;
            Object attachment = ctx.getAttachment();

            if (attachment == null) {
                // No context was added yet, add one
                dispatcherContext = new DispatcherContext();
                ctx.setAttachment(dispatcherContext);
            }
            else if (!(attachment instanceof DispatcherContext)) {
                // There was a context, but it was the wrong type. This should never happen.
                throw new IllegalStateException("NiftyDispatcher handler context should be of type NiftyDispatcher.DispatcherContext");
            }
            else {
                dispatcherContext = (DispatcherContext) attachment;
            }

            return dispatcherContext;
        }

        private enum ReadBlockedState {
            NOT_BLOCKED,
            BLOCKED,
        }
    }
}
