/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.metadata.ThriftType;

public class ByteThriftTypeCodec implements ThriftTypeCodec<Byte> {
  @Override
  public ThriftType getType() {
    return ThriftType.BYTE;
  }

  @Override
  public Byte read(TProtocolReader protocol) throws Exception {
    return protocol.readByte();
  }

  @Override
  public void write(Byte value, TProtocolWriter protocol) throws Exception {
    protocol.writeByte(value);
  }
}
