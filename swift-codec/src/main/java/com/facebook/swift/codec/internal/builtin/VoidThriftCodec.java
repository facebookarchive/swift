/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal.builtin;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.base.Preconditions;

/**
 * VoidThriftCodec is a convenience codec used for service invocations that return void type.
 */
public class VoidThriftCodec implements ThriftCodec<Void> {
  @Override
  public ThriftType getType() {
    return ThriftType.VOID;
  }

  /**
   * Always returns null without reading anything from the stream.
   */
  @Override
  public Void read(TProtocolReader protocol) throws Exception {
    Preconditions.checkNotNull(protocol, "protocol is null");
    return null;
  }

  /**
   * Always returns without writing to the stream.
   */
  @Override
  public void write(Void value, TProtocolWriter protocol) throws Exception {
    Preconditions.checkNotNull(protocol, "protocol is null");
  }
}
