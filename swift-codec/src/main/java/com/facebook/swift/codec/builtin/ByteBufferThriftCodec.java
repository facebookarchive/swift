/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.builtin;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;

public class ByteBufferThriftCodec implements ThriftCodec<ByteBuffer> {
  @Override
  public ThriftType getType() {
    return ThriftType.STRING;
  }

  @Override
  public ByteBuffer read(TProtocolReader protocol) throws Exception {
    Preconditions.checkNotNull(protocol, "protocol is null");
    return protocol.readBinary();
  }

  @Override
  public void write(ByteBuffer value, TProtocolWriter protocol) throws Exception {
    Preconditions.checkNotNull(value, "value is null");
    Preconditions.checkNotNull(protocol, "protocol is null");
    protocol.writeBinary(value);
  }
}
