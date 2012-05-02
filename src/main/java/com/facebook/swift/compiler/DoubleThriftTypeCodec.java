/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.ThriftProtocolFieldType;
import com.facebook.swift.metadata.ThriftType;

public class DoubleThriftTypeCodec implements ThriftTypeCodec<Double> {
  @Override
  public ThriftType getType() {
    return ThriftType.DOUBLE;
  }

  @Override
  public Double read(TProtocolReader protocol) throws Exception {
    return protocol.readDouble();
  }

  @Override
  public void write(Double value, TProtocolWriter protocol) throws Exception {
    protocol.writeDouble(value);
  }
}
