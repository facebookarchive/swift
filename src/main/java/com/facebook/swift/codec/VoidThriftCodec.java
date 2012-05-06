/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.internal.TProtocolReader;
import com.facebook.swift.internal.TProtocolWriter;
import com.facebook.swift.metadata.ThriftType;

public class VoidThriftCodec implements ThriftCodec<Void> {
  @Override
  public ThriftType getType() {
    return ThriftType.VOID;
  }

  @Override
  public Void read(TProtocolReader protocol) throws Exception {
    return null;
  }

  @Override
  public void write(Void value, TProtocolWriter protocol) throws Exception {
  }
}
