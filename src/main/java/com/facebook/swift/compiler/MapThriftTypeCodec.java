/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.metadata.ThriftType;

import java.util.Map;

public class MapThriftTypeCodec<K, V> implements ThriftTypeCodec<Map<K, V>> {
  private final ThriftTypeCodec<K> keyCodec;
  private final ThriftTypeCodec<V> valueCodec;
  private final ThriftType type;

  public MapThriftTypeCodec(ThriftTypeCodec<K> keyCodec, ThriftTypeCodec<V> valueCodec) {
    this.keyCodec = keyCodec;
    this.valueCodec = valueCodec;
    type = ThriftType.map(keyCodec.getType(), valueCodec.getType());
  }

  @Override
  public ThriftType getType() {
    return type;
  }

  @Override
  public Map<K, V> read(TProtocolReader protocol) throws Exception {
    return protocol.readMap(keyCodec, valueCodec);
  }

  @Override
  public void write(Map<K, V> value, TProtocolWriter protocol) throws Exception {
    protocol.writeMap(keyCodec, valueCodec, value);
  }
}
