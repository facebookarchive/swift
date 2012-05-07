/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.internal.TProtocolReader;
import com.facebook.swift.internal.TProtocolWriter;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.base.Preconditions;

import java.lang.reflect.Type;
import java.util.Set;

public class SetThriftCodec<T> implements ThriftCodec<Set<T>> {
  private final ThriftCodec<T> elementCodec;
  private final ThriftType type;

  public SetThriftCodec(ThriftType type, ThriftCodec<T> elementCodec) {
    Preconditions.checkNotNull(type, "type is null");
    Preconditions.checkNotNull(elementCodec, "elementCodec is null");

    this.type = type;
    this.elementCodec = elementCodec;
  }

  @Override
  public ThriftType getType() {
    return type;
  }

  @Override
  public Set<T> read(TProtocolReader protocol) throws Exception {
    Preconditions.checkNotNull(protocol, "protocol is null");
    return protocol.readSet(elementCodec);
  }

  @Override
  public void write(Set<T> value, TProtocolWriter protocol) throws Exception {
    Preconditions.checkNotNull(value, "value is null");
    Preconditions.checkNotNull(protocol, "protocol is null");
    protocol.writeSet(elementCodec, value);
  }
}
