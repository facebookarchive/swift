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
package com.facebook.swift.service;

import com.facebook.nifty.client.RequestChannel;
import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.core.RequestContexts;
import com.facebook.nifty.core.TChannelBufferInputTransport;
import com.facebook.nifty.core.TChannelBufferOutputTransport;
import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftFieldMetadata;
import com.facebook.swift.codec.metadata.ThriftParameterInjection;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.jboss.netty.buffer.ChannelBuffer;
import org.weakref.jmx.Managed;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Map;

import static org.apache.thrift.TApplicationException.BAD_SEQUENCE_ID;
import static org.apache.thrift.TApplicationException.INVALID_MESSAGE_TYPE;
import static org.apache.thrift.TApplicationException.WRONG_METHOD_NAME;
import static org.apache.thrift.protocol.TMessageType.CALL;
import static org.apache.thrift.protocol.TMessageType.EXCEPTION;
import static org.apache.thrift.protocol.TMessageType.ONEWAY;
import static org.apache.thrift.protocol.TMessageType.REPLY;

@ThreadSafe
public class ThriftMethodHandler
{
    private final String name;
    private final String qualifiedName;
    private final List<ParameterHandler> parameterCodecs;
    private final ThriftCodec<Object> successCodec;
    private final Map<Short, ThriftCodec<Object>> exceptionCodecs;
    private final boolean oneway;

    private final boolean invokeAsynchronously;

    public ThriftMethodHandler(ThriftMethodMetadata methodMetadata, ThriftCodecManager codecManager)
    {
        name = methodMetadata.getName();
        qualifiedName = methodMetadata.getQualifiedName();
        invokeAsynchronously = methodMetadata.isAsync();

        oneway = methodMetadata.getOneway();

        // get the thrift codecs for the parameters
        ParameterHandler[] parameters = new ParameterHandler[methodMetadata.getParameters().size()];
        for (ThriftFieldMetadata fieldMetadata : methodMetadata.getParameters()) {
            ThriftParameterInjection parameter = (ThriftParameterInjection) fieldMetadata.getInjections().get(0);

            ParameterHandler handler = new ParameterHandler(
                    fieldMetadata.getId(),
                    fieldMetadata.getName(),
                    (ThriftCodec<Object>) codecManager.getCodec(fieldMetadata.getThriftType()));

            parameters[parameter.getParameterIndex()] = handler;
        }
        parameterCodecs = ImmutableList.copyOf(parameters);

        // get the thrift codecs for the exceptions
        ImmutableMap.Builder<Short, ThriftCodec<Object>> exceptions = ImmutableMap.builder();
        for (Map.Entry<Short, ThriftType> entry : methodMetadata.getExceptions().entrySet()) {
            exceptions.put(entry.getKey(), (ThriftCodec<Object>) codecManager.getCodec(entry.getValue()));
        }
        exceptionCodecs = exceptions.build();

        // get the thrift codec for the return value
        successCodec = (ThriftCodec<Object>) codecManager.getCodec(methodMetadata.getReturnType());
    }

    @Managed
    public String getName()
    {
        return name;
    }

    public String getQualifiedName()
    {
        return qualifiedName;
    }

    public Object invoke(
            final RequestChannel channel,
            final TChannelBufferInputTransport inputTransport,
            final TChannelBufferOutputTransport outputTransport,
            final TProtocol inputProtocol,
            final TProtocol outputProtocol,
            final int sequenceId,
            final ClientContextChain contextChain,
            final Object... args)
            throws Exception
    {
        if (invokeAsynchronously)
        {
            // This method declares a Future return value: run it asynchronously
            return asynchronousInvoke(channel, inputTransport, outputTransport, inputProtocol, outputProtocol, sequenceId, contextChain, args);
        }
        else
        {
            try {
                // This method declares an immediate return value: run it synchronously
                return synchronousInvoke(channel, inputTransport, outputTransport, inputProtocol, outputProtocol, sequenceId, contextChain, args);
            }
            finally {
                contextChain.done();
            }
        }
    }

    private Object synchronousInvoke(
            RequestChannel channel,
            TChannelBufferInputTransport inputTransport,
            TChannelBufferOutputTransport outputTransport,
            TProtocol inputProtocol,
            TProtocol outputProtocol,
            int sequenceId,
            ClientContextChain contextChain,
            Object[] args)
            throws Exception
    {
        Object results = null;

        // write request
        contextChain.preWrite(args);
        outputTransport.resetOutputBuffer();
        writeArguments(outputProtocol, sequenceId, args);
        // Don't need to copy the output buffer for sync case
        ChannelBuffer requestBuffer = outputTransport.getOutputBuffer();
        contextChain.postWrite(args);

        if (!this.oneway) {
            ChannelBuffer responseBuffer;

            try {
                responseBuffer = SyncClientHelpers.sendSynchronousTwoWayMessage(channel, requestBuffer);
            } catch (Exception e) {
                contextChain.preReadException(e);
                throw e;
            }

            // read results
            contextChain.preRead();
            try {
                inputTransport.setInputBuffer(responseBuffer);
                waitForResponse(inputProtocol, sequenceId);
                results = readResponse(inputProtocol);
                contextChain.postRead(results);
            } catch (Exception e) {
                contextChain.postReadException(e);
                throw e;
            }
        } else {
            try {
                SyncClientHelpers.sendSynchronousOneWayMessage(channel, requestBuffer);
            } catch (Exception e) {
                throw e;
            }
        }

        return results;
    }

    public ListenableFuture<Object> asynchronousInvoke(
            final RequestChannel channel,
            final TChannelBufferInputTransport inputTransport,
            final TChannelBufferOutputTransport outputTransport,
            final TProtocol inputProtocol,
            final TProtocol outputProtocol,
            final int sequenceId,
            final ClientContextChain contextChain,
            final Object[] args)
        throws Exception
    {
        final AsyncMethodCallFuture<Object> future = AsyncMethodCallFuture.create(contextChain);
        final RequestContext requestContext = RequestContexts.getCurrentContext();

        contextChain.preWrite(args);
        outputTransport.resetOutputBuffer();
        writeArguments(outputProtocol, sequenceId, args);
        ChannelBuffer requestBuffer = outputTransport.getOutputBuffer().copy();
        contextChain.postWrite(args);

        // send message and setup listener to handle the response
        channel.sendAsynchronousRequest(requestBuffer, false, new RequestChannel.Listener() {
            @Override
            public void onRequestSent() {
                if (oneway) {
                    try {
                        future.set(null);
                    }
                    catch (Exception e) {
                        future.setException(e);
                    }
                }
            }

            @Override
            public void onResponseReceived(ChannelBuffer message) {
                RequestContext oldRequestContext = RequestContexts.getCurrentContext();
                RequestContexts.setCurrentContext(requestContext);
                try {
                    contextChain.preRead();
                    inputTransport.setInputBuffer(message);
                    waitForResponse(inputProtocol, sequenceId);
                    Object results = readResponse(inputProtocol);
                    contextChain.postRead(results);
                    future.set(results);
                }
                catch (Exception e) {
                    contextChain.postReadException(e);
                    future.setException(e);
                }
                finally {
                    RequestContexts.setCurrentContext(oldRequestContext);
                }
            }

            @Override
            public void onChannelError(TException e) {
                RequestContext oldRequestContext = RequestContexts.getCurrentContext();
                RequestContexts.setCurrentContext(requestContext);
                try {
                    contextChain.preReadException(e);
                    future.setException(e);
                } finally {
                    RequestContexts.setCurrentContext(oldRequestContext);
                }
            }
        });

        return future;
    }

    private Object readResponse(TProtocol in)
            throws Exception
    {
        TProtocolReader reader = new TProtocolReader(in);
        reader.readStructBegin();
        Object results = null;
        Exception exception = null;
        while (reader.nextField()) {
            if (reader.getFieldId() == 0) {
                results = reader.readField(successCodec);
            }
            else {
                ThriftCodec<Object> exceptionCodec = exceptionCodecs.get(reader.getFieldId());
                if (exceptionCodec != null) {
                    exception = (Exception) reader.readField(exceptionCodec);
                }
                else {
                    reader.skipFieldData();
                }
            }
        }
        reader.readStructEnd();
        in.readMessageEnd();

        if (exception != null) {
            throw exception;
        }

        if (successCodec.getType() == ThriftType.VOID) {
            // TODO: check for non-null return from a void function?
            return null;
        }

        if (results == null) {
            throw new TApplicationException(TApplicationException.MISSING_RESULT, name + " failed: unknown result");
        }
        return results;
    }

    private void writeArguments(TProtocol out, int sequenceId, Object[] args)
            throws Exception
    {
        // Note that though setting message type to ONEWAY can be helpful when looking at packet
        // captures, some clients always send CALL and so servers are forced to rely on the "oneway"
        // attribute on thrift method in the interface definition, rather than checking the message
        // type.
        out.writeMessageBegin(new TMessage(name, oneway ? ONEWAY : CALL, sequenceId));

        // write the parameters
        TProtocolWriter writer = new TProtocolWriter(out);
        writer.writeStructBegin(name + "_args");
        for (int i = 0; i < args.length; i++) {
            Object value = args[i];
            ParameterHandler parameter = parameterCodecs.get(i);
            writer.writeField(parameter.getName(), parameter.getId(), parameter.getCodec(), value);
        }
        writer.writeStructEnd();

        out.writeMessageEnd();
        out.getTransport().flush();
    }

    private void waitForResponse(TProtocol in, int sequenceId)
            throws TException
    {
        TMessage message = in.readMessageBegin();
        if (message.type == EXCEPTION) {
            TApplicationException exception = TApplicationException.read(in);
            in.readMessageEnd();
            throw exception;
        }
        if (message.type != REPLY) {
            throw new TApplicationException(INVALID_MESSAGE_TYPE,
                                            "Received invalid message type " + message.type + " from server");
        }
        if (!message.name.equals(this.name)) {
            throw new TApplicationException(WRONG_METHOD_NAME,
                                            "Wrong method name in reply: expected " + this.name + " but received " + message.name);
        }
        if (message.seqid != sequenceId) {
            throw new TApplicationException(BAD_SEQUENCE_ID, name + " failed: out of sequence response");
        }
    }

    private static final class ParameterHandler
    {
        private final short id;
        private final String name;
        private final ThriftCodec<Object> codec;

        private ParameterHandler(short id, String name, ThriftCodec<Object> codec)
        {
            this.id = id;
            this.name = name;
            this.codec = codec;
        }

        public short getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

        public ThriftCodec<Object> getCodec()
        {
            return codec;
        }
    }

    private static final class AsyncMethodCallFuture<T> extends AbstractFuture<T>
    {
        private final ClientContextChain contextChain;

        public static <T> AsyncMethodCallFuture<T> create(ClientContextChain contextChain)
        {
            return new AsyncMethodCallFuture<>(contextChain);
        }

        private AsyncMethodCallFuture(ClientContextChain contextChain) {
            this.contextChain = contextChain;
        }

        @Override
        public boolean set(@Nullable T value)
        {
            contextChain.done();
            return super.set(value);
        }

        @Override
        public boolean setException(Throwable throwable)
        {
            contextChain.done();
            return super.setException(throwable);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            // Async call futures represent requests running on some other service,
            // there is no way to cancel the request once it has been sent.
            return false;
        }
    }
}
