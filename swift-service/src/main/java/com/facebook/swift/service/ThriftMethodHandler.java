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
package com.facebook.swift.service;

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
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.weakref.jmx.Flatten;
import org.weakref.jmx.Managed;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Map;

import static io.airlift.units.Duration.nanosSince;
import static org.apache.thrift.TApplicationException.BAD_SEQUENCE_ID;
import static org.apache.thrift.protocol.TMessageType.CALL;
import static org.apache.thrift.protocol.TMessageType.EXCEPTION;
import static org.apache.thrift.protocol.TMessageType.ONEWAY;

@ThreadSafe
public class ThriftMethodHandler
{
    private final String name;
    private final List<ParameterHandler> parameterCodecs;
    private final ThriftCodec<Object> successCodec;
    private final Map<Short, ThriftCodec<Object>> exceptionCodecs;
    private final boolean oneway;

    private final ThriftMethodStats stats = new ThriftMethodStats();

    public ThriftMethodHandler(ThriftMethodMetadata methodMetadata, ThriftCodecManager codecManager)
    {
        name = methodMetadata.getName();

        oneway = methodMetadata.getOneway();

        // get the thrift codecs for the parameters
        ParameterHandler[] parameters = new ParameterHandler[methodMetadata.getParameters().size()];
        for (ThriftFieldMetadata fieldMetadata : methodMetadata.getParameters()) {
            ThriftParameterInjection parameter = (ThriftParameterInjection) fieldMetadata.getInjections().get(0);

            ParameterHandler handler = new ParameterHandler(
                    fieldMetadata.getId(),
                    fieldMetadata.getName(),
                    (ThriftCodec<Object>) codecManager.getCodec(fieldMetadata.getType()));

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

    @Managed
    @Flatten
    public ThriftMethodStats getStats()
    {
        return stats;
    }

    public Object invoke(final TProtocol in,
                         final TProtocol out,
                         final int sequenceId,
                         final Object... args)
            throws Exception
    {
        long start = System.nanoTime();

        try {
            Object results = null;

            // write request
            writeArguments(out, sequenceId, args);

            if (!this.oneway) {
                // wait for response message
                waitForResponse(in, sequenceId);

                // read results
                results = readResponse(in);
            }

            stats.addSuccessTime(nanosSince(start));
            return results;
        }
        catch (Exception e) {
            stats.addErrorTime(nanosSince(start));
            throw e;
        }

    }

    private Object readResponse(TProtocol in)
            throws Exception
    {
        long start = System.nanoTime();

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

        stats.addReadTime(nanosSince(start));

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
        long start = System.nanoTime();

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

        stats.addWriteTime(nanosSince(start));
    }

    private void waitForResponse(TProtocol in, int sequenceId)
            throws TException
    {
        long start = System.nanoTime();

        TMessage message = in.readMessageBegin();
        if (message.type == EXCEPTION) {
            TApplicationException exception = TApplicationException.read(in);
            in.readMessageEnd();
            throw exception;
        }
        if (message.seqid != sequenceId) {
            throw new TApplicationException(BAD_SEQUENCE_ID, name + " failed: out of sequence response");
        }

        stats.addInvokeTime(nanosSince(start));
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
}
