/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.apache.thrift.TApplicationException.*;

/**
 * Example TProcessor that wraps a Thrift service.  This should only be considered an example, and
 * is not production ready.  For example, this class makes assumptions about the thrift id of
 * method parameters, and does not support Thrift exceptions properly.
 */
public class ThriftServiceProcessor implements TProcessor {
  private final Object service;
  private final ThriftServiceMetadata serviceMetadata;
  private final ThriftCodecManager codecManager;

  public ThriftServiceProcessor(
      Object service,
      ThriftCodecManager codecManager
  ) {
    this(service,
        codecManager,
        new ThriftServiceMetadata(service.getClass(), codecManager.getCatalog()));
  }

  public ThriftServiceProcessor(
      Object service,
      ThriftCodecManager codecManager, ThriftServiceMetadata serviceMetadata
  ) {
    Preconditions.checkNotNull(service, "service is null");
    Preconditions.checkNotNull(serviceMetadata, "serviceMetadata is null");
    Preconditions.checkNotNull(codecManager, "codecManager is null");

    this.service = service;
    this.serviceMetadata = serviceMetadata;
    this.codecManager = codecManager;
  }

  @Override
  public boolean process(TProtocol in, TProtocol out) throws TException {
    TMessage message = in.readMessageBegin();
    String methodName = message.name;
    int sequenceId = message.seqid;

    try {
      // lookup method
      ThriftMethodMetadata methodMetadata = serviceMetadata.getMethod(methodName);
      if (methodMetadata == null) {
        TProtocolUtil.skip(in, TType.STRUCT);
        in.readMessageEnd();
        throw new TApplicationException(
            UNKNOWN_METHOD,
            "Invalid method name: '" + methodName + "'"
        );
      }
      Method method = methodMetadata.getMethod();

      // read args
      Object[] args = new Object[method.getParameterTypes().length];
      TProtocolReader reader = new TProtocolReader(in);
      try {
        reader.readStructBegin();
        while (reader.nextField()) {
          short fieldId = reader.getFieldId();
          ThriftCodec<?> codec = codecManager.getCodec(method.getGenericParameterTypes()[fieldId - 1]);
          args[fieldId - 1] = reader.readField(codec);
        }
        reader.readStructEnd();
      } catch (Exception e) {
        throw new TApplicationException(PROTOCOL_ERROR, e.getMessage());
      }

      // invoke method
      Object result;
      try {
        result = method.invoke(service, args);
      } catch (Throwable e) {
        if (e instanceof InvocationTargetException) {
          InvocationTargetException invocationTargetException = (InvocationTargetException) e;
          if (invocationTargetException.getTargetException() != null) {
            e = invocationTargetException.getTargetException();
          }
        }
        Throwables.propagateIfInstanceOf(e, Exception.class);
        throw Throwables.propagate(e);
      }

      // write the response
      ThriftCodec<Object> resultCodec =
          (ThriftCodec<Object>) codecManager.getCodec(methodMetadata.getReturnType());

      out.writeMessageBegin(new TMessage(methodName, TMessageType.REPLY, sequenceId));
      try {
        TProtocolWriter writer = new TProtocolWriter(out);
        writer.writeStructBegin(methodName + "_result");
        writer.writeField("result", (short) 0, resultCodec, result);
        writer.writeStructEnd();
      } catch (Exception e) {
        throw new TProtocolException(e);
      }
      out.writeMessageEnd();
      out.getTransport().flush();

      return true;
    } catch (Exception e) {
      TApplicationException exception;
      if (e instanceof  TApplicationException) {
        exception = (TApplicationException) e;
      } else {
        exception = new TApplicationException(INTERNAL_ERROR, e.getMessage());
      }
      out.writeMessageBegin(new TMessage(methodName, TMessageType.EXCEPTION, sequenceId));
      exception.write(out);
      out.writeMessageEnd();
      out.getTransport().flush();
      return true;
    } finally {
      try {
        in.readMessageEnd();
      } catch (TException ignore) {
      }
    }
  }
}
