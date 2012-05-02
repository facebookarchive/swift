/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftType;

public class LongThriftCodec implements ThriftCodec<Long> {
  @Override
  public ThriftType getType() {
    return ThriftType.I64;
  }

  @Override
  public Long read(TProtocolReader protocol) throws Exception {
    return protocol.readI64();
  }

  @Override
  public void write(Long value, TProtocolWriter protocol) throws Exception {
    protocol.writeI64(value);
  }
}
