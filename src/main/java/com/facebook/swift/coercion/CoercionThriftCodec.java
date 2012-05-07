/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.coercion;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.internal.TProtocolReader;
import com.facebook.swift.internal.TProtocolWriter;
import com.facebook.swift.metadata.ThriftType;
import com.facebook.swift.metadata.TypeCoercion;

/**
 * CoercionThriftCodec encapsulates a ThriftCodec and coerces the values to another type using
 * the supplied ThriftCoercion.
 */
public class CoercionThriftCodec<T> implements ThriftCodec<T> {
  private final ThriftCodec<Object> codec;
  private final TypeCoercion typeCoercion;
  private final ThriftType thriftType;

  public CoercionThriftCodec(
      ThriftCodec<?> codec,
      TypeCoercion typeCoercion
  ) {
    this.codec = (ThriftCodec<Object>) codec;
    this.typeCoercion = typeCoercion;
    this.thriftType = typeCoercion.getThriftType();
  }

  @Override
  public ThriftType getType() {
    return thriftType;
  }

  @Override
  public T read(TProtocolReader protocol) throws Exception {
    Object thriftValue = codec.read(protocol);
    T javaValue = (T) typeCoercion.getFromThrift().invoke(null, thriftValue);
    return javaValue;
  }

  @Override
  public void write(T javaValue, TProtocolWriter protocol) throws Exception {
    Object thriftValue = typeCoercion.getToThrift().invoke(null, javaValue);
    codec.write(thriftValue, protocol);
  }
}
