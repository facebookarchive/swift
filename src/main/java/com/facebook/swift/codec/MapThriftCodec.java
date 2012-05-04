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
    return protocol.readMap(keyCodec, valueCodec);
  }

  @Override
  public void write(Map<K, V> value, TProtocolWriter protocol) throws Exception {
    protocol.writeMap(keyCodec, valueCodec, value);
  }
}
