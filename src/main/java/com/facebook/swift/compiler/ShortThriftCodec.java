/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftType;

public class ShortThriftCodec implements ThriftCodec<Short> {
  @Override
  public ThriftType getType() {
    return ThriftType.I16;
  }

  @Override
  public Short read(TProtocolReader protocol) throws Exception {
    return protocol.readI16();
  }

  @Override
  public void write(Short value, TProtocolWriter protocol) throws Exception {
    protocol.writeI16(value);
  }
}
