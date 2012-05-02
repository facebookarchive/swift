/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.internal.TProtocolReader;
import com.facebook.swift.internal.TProtocolWriter;
import com.facebook.swift.metadata.ThriftType;

public class StringThriftCodec implements ThriftCodec<String> {
  @Override
  public ThriftType getType() {
    return ThriftType.STRING;
  }

  @Override
  public String read(TProtocolReader protocol) throws Exception {
    return protocol.readString();
  }

  @Override
  public void write(String value, TProtocolWriter protocol) throws Exception {
    protocol.writeString(value);
  }
}
