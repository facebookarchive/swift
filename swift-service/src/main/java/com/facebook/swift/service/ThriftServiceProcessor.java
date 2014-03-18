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

import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.thrift.TApplicationException.INVALID_MESSAGE_TYPE;
import static org.apache.thrift.TApplicationException.UNKNOWN_METHOD;

/**
 * Example TProcessor that wraps a Thrift service.  This should only be considered an example, and
 * is not production ready.  For example, this class makes assumptions about the thrift id of
 * method parameters, and does not support Thrift exceptions properly.
 */
@ThreadSafe
public class ThriftServiceProcessor implements NiftyProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(ThriftServiceProcessor.class);

    private final Map<String, ThriftMethodProcessor> methods;
    private final List<ThriftEventHandler> eventHandlers;

    /**
     * @param eventHandlers event handlers to attach to services
     * @param services the services to expose; services must be thread safe
     */
    public ThriftServiceProcessor(ThriftCodecManager codecManager, List<? extends ThriftEventHandler> eventHandlers, Object... services)
    {
        this(codecManager, eventHandlers, ImmutableList.copyOf(services));
    }

    public ThriftServiceProcessor(ThriftCodecManager codecManager, List<? extends ThriftEventHandler> eventHandlers, List<?> services)
    {
        Preconditions.checkNotNull(codecManager, "codecManager is null");
        Preconditions.checkNotNull(services, "service is null");
        Preconditions.checkArgument(!services.isEmpty(), "services is empty");

        Map<String, ThriftMethodProcessor> processorMap = newHashMap();
        for (Object service : services) {
            ThriftServiceMetadata serviceMetadata = new ThriftServiceMetadata(service.getClass(), codecManager.getCatalog());
            for (ThriftMethodMetadata methodMetadata : serviceMetadata.getMethods().values()) {
                String methodName = methodMetadata.getName();
                ThriftMethodProcessor methodProcessor = new ThriftMethodProcessor(service, serviceMetadata.getName(), methodMetadata, codecManager);
                if (processorMap.containsKey(methodName)) {
                    throw new IllegalArgumentException("Multiple @ThriftMethod-annotated methods named '" + methodName + "' found in the given services");
                }
                processorMap.put(methodName, methodProcessor);
            }
        }
        methods = ImmutableMap.copyOf(processorMap);
        this.eventHandlers = ImmutableList.copyOf(eventHandlers);
    }

    public Map<String, ThriftMethodProcessor> getMethods()
    {
        return methods;
    }

    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public ListenableFuture<Boolean> process(final TProtocol in, TProtocol out, RequestContext requestContext)
            throws TException
    {
        try {
            final SettableFuture<Boolean> resultFuture = SettableFuture.create();
            TMessage message = in.readMessageBegin();
            String methodName = message.name;
            int sequenceId = message.seqid;

            // lookup method
            ThriftMethodProcessor method = methods.get(methodName);
            if (method == null) {
                TProtocolUtil.skip(in, TType.STRUCT);
                writeApplicationException(out, methodName, sequenceId, UNKNOWN_METHOD, "Invalid method name: '" + methodName + "'", null);
                return Futures.immediateFuture(true);
            }

            switch (message.type) {
                case TMessageType.CALL:
                case TMessageType.ONEWAY:
                    // Ideally we'd check the message type here to make the presence/absence of
                    // the "oneway" keyword annotating the method matches the message type.
                    // Unfortunately most clients send both one-way and two-way messages as CALL
                    // message type instead of using ONEWAY message type, and servers ignore the
                    // difference.
                    break;

                default:
                    TProtocolUtil.skip(in, TType.STRUCT);
                    writeApplicationException(out, methodName, sequenceId, INVALID_MESSAGE_TYPE, "Received invalid message type " + message.type + " from client", null);
                    return Futures.immediateFuture(true);
            }

            // invoke method
            final ContextChain context = new ContextChain(eventHandlers, method.getQualifiedName(), requestContext);
            ListenableFuture<Boolean> processResult = method.process(in, out, sequenceId, context);

            Futures.addCallback(
                    processResult,
                    new FutureCallback<Boolean>()
                    {
                        @Override
                        public void onSuccess(Boolean result)
                        {
                            context.done();
                            resultFuture.set(result);
                        }

                        @Override
                        public void onFailure(Throwable t)
                        {
                            context.done();
                            resultFuture.setException(t);
                        }
                    });

            return resultFuture;
        }
        catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public static TApplicationException writeApplicationException(
            TProtocol outputProtocol,
            String methodName,
            int sequenceId,
            int errorCode,
            String errorMessage,
            Throwable cause)
            throws TException
    {
        // unexpected exception
        TApplicationException applicationException = new TApplicationException(errorCode, errorMessage);
        if (cause != null) {
            applicationException.initCause(cause);
        }

        LOG.error(errorMessage, applicationException);

        // Application exceptions are sent to client, and the connection can be reused
        outputProtocol.writeMessageBegin(new TMessage(methodName, TMessageType.EXCEPTION, sequenceId));
        applicationException.write(outputProtocol);
        outputProtocol.writeMessageEnd();
        outputProtocol.getTransport().flush();

        return applicationException;
    }
}
