/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal.builtin;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.base.Preconditions;

public class ShortThriftCodec implements ThriftCodec<Short> {
  @Override
  public ThriftType getType() {
    return ThriftType.I16;
  }

  @Override
  public Short read(TProtocolReader protocol) throws Exception {
    Preconditions.checkNotNull(protocol, "protocol is null");
    return protocol.readI16();
  }

  @Override
  public void write(Short value, TProtocolWriter protocol) throws Exception {
    Preconditions.checkNotNull(value, "value is null");
    Preconditions.checkNotNull(protocol, "protocol is null");
    protocol.writeI16(value);
  }
}
