/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftType;

import java.util.Set;

public class SetThriftCodec<T> implements ThriftCodec<Set<T>> {
  private final ThriftCodec<T> elementCodec;
  private final ThriftType type;

  public SetThriftCodec(ThriftCodec<T> elementCodec) {
    this.elementCodec = elementCodec;
    type = ThriftType.set(elementCodec.getType());
  }

  @Override
  public ThriftType getType() {
    return type;
  }

  @Override
  public Set<T> read(TProtocolReader protocol) throws Exception {
    return protocol.readSet(elementCodec);
  }

  @Override
  public void write(Set<T> value, TProtocolWriter protocol) throws Exception {
    protocol.writeSet(elementCodec, value);
  }
}
