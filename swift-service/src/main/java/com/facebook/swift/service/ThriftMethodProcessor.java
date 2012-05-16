/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftFieldMetadata;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolException;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.apache.thrift.TApplicationException.INTERNAL_ERROR;

@ThreadSafe
public class ThriftMethodProcessor {
  private final String name;
  private final Object service;
  private final Method method;
  private final String resultStructName;
  private final Map<Short, ThriftCodec<?>> parameterCodecs;
  private final ThriftCodec<Object> successCodec;
  private final Map<Class<?>, ExceptionProcessor> exceptionCodecs;

  public ThriftMethodProcessor(
      Object service,
      ThriftMethodMetadata methodMetadata,
      ThriftCodecManager codecManager
  ) {
    this.service = service;

    name = methodMetadata.getName();
    resultStructName = name + "_result";

    method = methodMetadata.getMethod();

    ImmutableMap.Builder<Short, ThriftCodec<?>> builder = ImmutableMap.builder();
    for (ThriftFieldMetadata fieldMetadata : methodMetadata.getParameters()) {
      builder.put(fieldMetadata.getId(), codecManager.getCodec(fieldMetadata.getType()));
    }
    parameterCodecs = builder.build();

    ImmutableMap.Builder<Class<?>, ExceptionProcessor> exceptions = ImmutableMap.builder();
    for (Map.Entry<Short, ThriftType> entry : methodMetadata.getExceptions().entrySet()) {
      Class<?> type = TypeToken.of(entry.getValue().getJavaType()).getRawType();
      ExceptionProcessor processor = new ExceptionProcessor(
          entry.getKey(),
          codecManager.getCodec(entry.getValue())
      );
      exceptions.put(type, processor);
    }
    exceptionCodecs = exceptions.build();

    successCodec = (ThriftCodec<Object>) codecManager.getCodec(methodMetadata.getReturnType());

  }

  public void process(TProtocol in, TProtocol out, int sequenceId) throws Exception {
    // read args
    Object[] args;
    try {
      args = new Object[method.getParameterTypes().length];
      TProtocolReader reader = new TProtocolReader(in);

      reader.readStructBegin();
      while (reader.nextField()) {
        short fieldId = reader.getFieldId();

        ThriftCodec<?> codec = parameterCodecs.get(fieldId);
        if (codec == null) {
          // unknown field
          reader.skipFieldData();
        }

        args[fieldId - 1] = reader.readField(codec);
      }
      reader.readStructEnd();
    } catch (TProtocolException e) {
      // TProtocolException is the only recoverable exception
      // Other exceptions may have left the input stream in corrupted state so we must
      // tear down the socket.
      throw new TApplicationException(TApplicationException.PROTOCOL_ERROR, e.getMessage());
    }

    // invoke method
    String responseFieldName = "success";
    short responseFieldId = 0;
    ThriftCodec<Object> responseCodec = successCodec;
    Object result;
    try {
      result = method.invoke(service, args);
    } catch (Throwable e) {
      // strip off the InvocationTargetException wrapper if present
      if (e instanceof InvocationTargetException) {
        InvocationTargetException invocationTargetException = (InvocationTargetException) e;
        if (invocationTargetException.getTargetException() != null) {
          e = invocationTargetException.getTargetException();
        }
      }

      ExceptionProcessor exceptionCodec = exceptionCodecs.get(e.getClass());
      if (exceptionCodec != null) {
        result = e;
        responseFieldId = exceptionCodec.getId();
        responseCodec = exceptionCodec.getCodec();
      } else {
        TApplicationException applicationException = new TApplicationException(
            INTERNAL_ERROR,
            "Internal error processing " + method.getName()
        );
        applicationException.initCause(e);
        throw applicationException;
      }
    }

    // write the response
    out.writeMessageBegin(new TMessage(name, TMessageType.REPLY, sequenceId));

    TProtocolWriter writer = new TProtocolWriter(out);
    writer.writeStructBegin(resultStructName);
    writer.writeField(responseFieldName, responseFieldId, responseCodec, result);
    writer.writeStructEnd();

    out.writeMessageEnd();
    out.getTransport().flush();
  }

  private static final class ExceptionProcessor {
    private final short id;
    private final ThriftCodec<Object> codec;

    private ExceptionProcessor(short id, ThriftCodec<?> coded) {
      this.id = id;
      this.codec = (ThriftCodec<Object>) coded;
    }

    public short getId() {
      return id;
    }

    public ThriftCodec<Object> getCodec() {
      return codec;
    }
  }
}
