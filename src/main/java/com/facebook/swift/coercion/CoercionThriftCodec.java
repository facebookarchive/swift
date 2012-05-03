/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.coercion;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.internal.TProtocolReader;
import com.facebook.swift.internal.TProtocolWriter;
import com.facebook.swift.metadata.JavaToThriftCoercion;
import com.facebook.swift.metadata.ThriftToJavaCoercion;
import com.facebook.swift.metadata.ThriftType;

public class CoercionThriftCodec<J> implements ThriftCodec<J> {
  private final ThriftCodec<Object> codec;
  private final JavaToThriftCoercion toThriftCoercion;
  private final ThriftToJavaCoercion fromThriftCoercion;
  private final ThriftType thriftType;

  public CoercionThriftCodec(
      ThriftCodec<?> codec,
      JavaToThriftCoercion toThriftCoercion,
      ThriftToJavaCoercion fromThriftCoercion
  ) {
    this.codec = (ThriftCodec<Object>) codec;
    this.toThriftCoercion = toThriftCoercion;
    this.fromThriftCoercion = fromThriftCoercion;
    this.thriftType = toThriftCoercion.getThriftType();
  }

  @Override
  public ThriftType getType() {
    return thriftType;
  }

  @Override
  public J read(TProtocolReader protocol) throws Exception {
    Object thriftValue = codec.read(protocol);
    J javaValue = (J) fromThriftCoercion.getMethod().invoke(null, thriftValue);
    return javaValue;
  }

  @Override
  public void write(J javaValue, TProtocolWriter protocol) throws Exception {
    Object thriftValue = toThriftCoercion.getMethod().invoke(null, javaValue);
    codec.write(thriftValue, protocol);
  }
}
