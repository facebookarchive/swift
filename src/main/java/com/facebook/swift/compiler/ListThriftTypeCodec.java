/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.metadata.ThriftType;

import java.util.List;
import java.util.Set;

public class ListThriftTypeCodec<T> implements ThriftTypeCodec<List<T>> {
  private final ThriftTypeCodec<T> elementCodec;
  private final ThriftType type;

  public ListThriftTypeCodec(ThriftTypeCodec<T> elementCodec) {
    this.elementCodec = elementCodec;
    type = ThriftType.list(elementCodec.getType());
  }

  @Override
  public ThriftType getType() {
    return type;
  }

  @Override
  public List<T> read(TProtocolReader protocol) throws Exception {
    return protocol.readList(elementCodec);
  }

  @Override
  public void write(List<T> value, TProtocolWriter protocol) throws Exception {
    protocol.writeList(elementCodec, value);
  }
}
