/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.internal.TProtocolReader;
import com.facebook.swift.internal.TProtocolWriter;
import com.facebook.swift.metadata.ThriftType;

public class BooleanThriftCodec implements ThriftCodec<Boolean> {
  @Override
  public ThriftType getType() {
    return ThriftType.BOOL;
  }

  @Override
  public Boolean read(TProtocolReader protocol) throws Exception {
    return protocol.readBool();
  }

  @Override
  public void write(Boolean value, TProtocolWriter protocol) throws Exception {
    protocol.writeBool(value);
  }
}
