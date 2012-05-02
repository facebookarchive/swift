/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftType;

import java.nio.ByteBuffer;

public class ByteBufferThriftCodec implements ThriftCodec<ByteBuffer> {
  @Override
  public ThriftType getType() {
    return ThriftType.STRING;
  }

  @Override
  public ByteBuffer read(TProtocolReader protocol) throws Exception {
    return protocol.readBinary();
  }

  @Override
  public void write(ByteBuffer value, TProtocolWriter protocol) throws Exception {
    protocol.writeBinary(value);
  }
}
