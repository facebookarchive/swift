/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftType;

public class IntegerThriftCodec implements ThriftCodec<Integer> {
  @Override
  public ThriftType getType() {
    return ThriftType.I32;
  }

  @Override
  public Integer read(TProtocolReader protocol) throws Exception {
    return protocol.readI32();
  }

  @Override
  public void write(Integer value, TProtocolWriter protocol) throws Exception {
    protocol.writeI32(value);
  }
}
