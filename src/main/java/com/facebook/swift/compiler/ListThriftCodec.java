/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftType;

import java.util.List;

public class ListThriftCodec<T> implements ThriftCodec<List<T>> {
  private final ThriftCodec<T> elementCodec;
  private final ThriftType type;

  public ListThriftCodec(ThriftCodec<T> elementCodec) {
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
