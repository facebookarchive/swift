/*
 * Copyright 2004-present Facebook. All Rights Reserved.
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
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Map;

import static org.apache.thrift.TApplicationException.BAD_SEQUENCE_ID;
import static org.apache.thrift.protocol.TMessageType.CALL;
import static org.apache.thrift.protocol.TMessageType.EXCEPTION;

@ThreadSafe
public class ThriftMethodHandler {
  private final String name;
  private final List<ParameterHandler> parameterCodecs;
  private final ThriftCodec<Object> successCodec;
  private final Map<Short, ThriftCodec<Object>> exceptionCodecs;

  private int sequenceId;

  public ThriftMethodHandler(
      ThriftMethodMetadata methodMetadata,
      ThriftCodecManager codecManager
  ) {

    name = methodMetadata.getName();

    ParameterHandler[] parameters = new ParameterHandler[methodMetadata.getParameters().size()];
    for (ThriftFieldMetadata fieldMetadata : methodMetadata.getParameters()) {
      ThriftParameterInjection parameter = (ThriftParameterInjection) fieldMetadata.getInjections()
          .get(0);

      ParameterHandler handler = new ParameterHandler(
          fieldMetadata.getId(),
          fieldMetadata.getName(),
          (ThriftCodec<Object>) codecManager.getCodec(fieldMetadata.getType())
      );

      parameters[parameter.getParameterIndex()] = handler;
    }
    parameterCodecs = ImmutableList.copyOf(parameters);

    ImmutableMap.Builder<Short, ThriftCodec<Object>> exceptions = ImmutableMap.builder();
    for (Map.Entry<Short, ThriftType> entry : methodMetadata.getExceptions().entrySet()) {
      exceptions.put(entry.getKey(), (ThriftCodec<Object>) codecManager.getCodec(entry.getValue()));
    }
    exceptionCodecs = exceptions.build();

    successCodec = (ThriftCodec<Object>) codecManager.getCodec(methodMetadata.getReturnType());

  }

  public Object invoke(TProtocol in, TProtocol out, Object... args) throws Throwable {
    // write request
    int sequenceId = this.sequenceId++;
    out.writeMessageBegin(new TMessage(name, CALL, sequenceId));

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

    // check response message
    TMessage message = out.readMessageBegin();
    if (message.type == EXCEPTION) {
      TApplicationException exception = TApplicationException.read(in);
      in.readMessageEnd();
      throw exception;
    }
    if (message.seqid != sequenceId) {
      throw new TApplicationException(
          BAD_SEQUENCE_ID,
          name + " failed: out of sequence response"
      );
    }

    // read results
    TProtocolReader reader = new TProtocolReader(out);
    reader.readStructBegin();
    Object results = null;
    Exception exception = null;
    while (reader.nextField()) {
      if (reader.getFieldId() == 0) {
        results = reader.readField(successCodec);
      } else {
        ThriftCodec<Object> exceptionCodec = exceptionCodecs.get(reader.getFieldId());
        if (exceptionCodec != null) {
          exception = (Exception) reader.readField(exceptionCodec);
        } else {
          reader.skipFieldData();
        }
      }
    }
    reader.readStructEnd();
    out.readMessageEnd();
    if (exception != null) {
      throw exception;
    }
    if (results == null) {
      // todo how is void handled?
      throw new TApplicationException(
          TApplicationException.MISSING_RESULT,
          name + " failed: unknown result"
      );
    }
    return results;

  }

  private static final class ParameterHandler {
    private final short id;
    private final String name;
    private final ThriftCodec<Object> codec;

    private ParameterHandler(short id, String name, ThriftCodec<Object> codec) {
      this.id = id;
      this.name = name;
      this.codec = codec;
    }

    public short getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public ThriftCodec<Object> getCodec() {
      return codec;
    }
  }
}
