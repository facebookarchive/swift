/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal.builtin;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.base.Preconditions;

import java.util.Map;

public class MapThriftCodec<K, V> implements ThriftCodec<Map<K, V>> {
  private final ThriftType thriftType;
  private final ThriftCodec<K> keyCodec;
  private final ThriftCodec<V> valueCodec;

  public MapThriftCodec(ThriftType type, ThriftCodec<K> keyCodec, ThriftCodec<V> valueCodec) {
    Preconditions.checkNotNull(type, "type is null");
    Preconditions.checkNotNull(keyCodec, "keyCodec is null");
    Preconditions.checkNotNull(valueCodec, "valueCodec is null");

    this.thriftType = type;
    this.keyCodec = keyCodec;
    this.valueCodec = valueCodec;
  }

  @Override
  public ThriftType getType() {
    return thriftType;
  }

  @Override
  public Map<K, V> read(TProtocolReader protocol) throws Exception {
    Preconditions.checkNotNull(protocol, "protocol is null");
    return protocol.readMap(keyCodec, valueCodec);
  }

  @Override
  public void write(Map<K, V> value, TProtocolWriter protocol) throws Exception {
    Preconditions.checkNotNull(value, "value is null");
    Preconditions.checkNotNull(protocol, "protocol is null");
    protocol.writeMap(keyCodec, valueCodec, value);
  }
}
