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
package com.facebook.nifty.client;

import com.facebook.nifty.core.TChannelBufferInputTransport;
import com.facebook.nifty.core.TChannelBufferOutputTransport;
import com.google.common.base.Throwables;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;

@NotThreadSafe
public class TNiftyClientChannelTransport extends TTransport
{
    private final Class<? extends TServiceClient> clientClass;
    private final NiftyClientChannel channel;
    private final Map<String, Boolean> methodNameToOneWay;
    private final TChannelBufferOutputTransport requestBufferTransport;
    private final TChannelBufferInputTransport responseBufferTransport;
    private final BlockingQueue<ResponseListener> queuedResponses;

    public TNiftyClientChannelTransport(
            Class<? extends TServiceClient> clientClass, NiftyClientChannel channel)
    {
        this.clientClass = clientClass;
        this.channel = channel;

        this.methodNameToOneWay = newHashMap();
        this.requestBufferTransport = new TChannelBufferOutputTransport();
        this.responseBufferTransport = new TChannelBufferInputTransport(ChannelBuffers.buffer(0));
        this.queuedResponses = Queues.newLinkedBlockingQueue();
    }

    @Override
    public boolean isOpen()
    {
        return channel.getNettyChannel().isOpen();
    }

    @Override
    public void open()
            throws TTransportException
    {
        if (!isOpen()) {
            throw new IllegalStateException("TNiftyClientChannelTransport requires an already-opened channel");
        }
    }

    @Override
    public void close()
    {
        channel.close();
    }

    @Override
    public int read(byte[] buf, int off, int len)
            throws TTransportException
    {
        if (!responseBufferTransport.isReadable()) {
            try {
                // If our existing response transport doesn't have any bytes remaining to read,
                // wait for the next queued response to arrive, and point our response transport
                // to that.
                ResponseListener listener = queuedResponses.take();
                ChannelBuffer response = listener.getResponse().get();

                // Ensure the response buffer is not zero-sized
                checkState(response.readable(), "Received an empty response");

                responseBufferTransport.setInputBuffer(response);
            }
            catch (InterruptedException e) {
                // Waiting for response was interrupted
                Thread.currentThread().interrupt();
                throw new TTransportException(e);
            }
            catch (ExecutionException e) {
                // Error while waiting for response
                Throwables.propagateIfInstanceOf(e, TTransportException.class);
                throw new TTransportException(e);
            }
        }

        // Read as many bytes as we can (up to the amount requested) from the response
        return responseBufferTransport.read(buf, off, len);
    }

    @Override
    public void write(byte[] buf, int off, int len)
            throws TTransportException
    {
        // Write the buffer into the output transport
        requestBufferTransport.write(buf, off, len);
    }

    @Override
    public void flush()
            throws TTransportException
    {
        try {
            boolean sendOneWay = inOneWayRequest();
            ResponseListener listener = new ResponseListener();
            channel.sendAsynchronousRequest(requestBufferTransport.getOutputBuffer().copy(), sendOneWay, listener);
            queuedResponses.add(listener);
            requestBufferTransport.resetOutputBuffer();
        }
        catch (TException e) {
            Throwables.propagateIfInstanceOf(e, TTransportException.class);
            throw new TTransportException(TTransportException.UNKNOWN, "Failed to use reflection on Client class to determine whether method is oneway", e);
        }
    }

    private boolean inOneWayRequest()
            throws TException
    {
        boolean isOneWayMethod = false;

        // Create a temporary transport wrapping the output buffer, so that we can read the method name for this message
        TChannelBufferInputTransport requestReadTransport = new TChannelBufferInputTransport(requestBufferTransport.getOutputBuffer().duplicate());
        TProtocol protocol = channel.getProtocolFactory().getOutputProtocolFactory().getProtocol(requestReadTransport);
        TMessage message = protocol.readMessageBegin();
        String methodName = message.name;

        isOneWayMethod = clientClassHasReceiveHelperMethod(methodName);

        return isOneWayMethod;
    }

    private boolean clientClassHasReceiveHelperMethod(String methodName)
    {
        boolean isOneWayMethod = false;

        if (!methodNameToOneWay.containsKey(methodName)) {
            try {
                // HACK! We need to know whether the function is one-way, so we can tell the channel
                // whether to setup a response callback, but an implementation of TTransport doesn't
                // normally get this information, so we use reflection to look for a method like
                // 'recv_foo' which will only be generated for two-way functions.
                //
                // We should fix this by getting flushMessage()/flushOneWayMessage() added to
                // TTransport.
                clientClass.getMethod("recv_" + methodName);
            }
            catch (NoSuchMethodException e) {
                isOneWayMethod = true;
            }

            // cache result so we don't use reflection every time
            methodNameToOneWay.put(methodName, isOneWayMethod);
        }
        else  {
            isOneWayMethod = methodNameToOneWay.get(methodName);
        }
        return isOneWayMethod;
    }

    private static class ResponseListener implements NiftyClientChannel.Listener
    {
        private final SettableFuture<ChannelBuffer> response;

        private ResponseListener()
        {
            this.response = SettableFuture.create();
        }

        @Override
        public void onRequestSent()
        {
        }

        @Override
        public void onResponseReceived(ChannelBuffer response)
        {
            this.response.set(response);
        }

        @Override
        public void onChannelError(TException cause)
        {
            response.setException(new TTransportException(TTransportException.UNKNOWN, cause));
        }

        public ListenableFuture<ChannelBuffer> getResponse()
        {
            return response;
        }
    }
}
