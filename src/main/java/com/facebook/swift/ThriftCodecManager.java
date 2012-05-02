/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift;

import com.facebook.swift.codec.BooleanThriftCodec;
import com.facebook.swift.codec.ByteBufferThriftCodec;
import com.facebook.swift.codec.ByteThriftCodec;
import com.facebook.swift.internal.compiler.CompilerThriftCodecFactory;
import com.facebook.swift.codec.DoubleThriftCodec;
import com.facebook.swift.codec.IntegerThriftCodec;
import com.facebook.swift.codec.ListThriftCodec;
import com.facebook.swift.codec.LongThriftCodec;
import com.facebook.swift.codec.MapThriftCodec;
import com.facebook.swift.codec.SetThriftCodec;
import com.facebook.swift.codec.ShortThriftCodec;
import com.facebook.swift.codec.StringThriftCodec;
import com.facebook.swift.internal.TProtocolReader;
import com.facebook.swift.internal.TProtocolWriter;
import com.facebook.swift.internal.ThriftCodecFactory;
import com.facebook.swift.metadata.ThriftCatalog;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.thrift.protocol.TProtocol;

import java.util.concurrent.ExecutionException;

public class ThriftCodecManager {
  private final ThriftCatalog catalog;
  private final LoadingCache<ThriftType, ThriftCodec<?>> typeCodecs;
  private final ThriftCodecFactory factory;

  public ThriftCodecManager(ThriftCodec<?>... codecs) {
    this(new CompilerThriftCodecFactory(), codecs);
  }

  public ThriftCodecManager(ThriftCodecFactory factory, ThriftCodec<?>... codecs) {
    this(factory, new ThriftCatalog(), codecs);
  }

  public ThriftCodecManager(
      ThriftCodecFactory factory,
      ThriftCatalog catalog,
      ThriftCodec<?>... codecs
  ) {
    this.catalog = catalog;
    this.factory = factory;

    typeCodecs = CacheBuilder.newBuilder().build(
        new CacheLoader<ThriftType, ThriftCodec<?>>() {
          public ThriftCodec<?> load(ThriftType type) throws Exception {
            switch (type.getProtocolType()) {
              case STRUCT: {
                return ThriftCodecManager.this.factory.generateThriftTypeCodec(
                    ThriftCodecManager.this,
                    type.getStructMetadata()
                );
              }
              case MAP: {
                ThriftCodec<?> keyCodec = typeCodecs.get(type.getKeyType());
                ThriftCodec<?> valueCodec = typeCodecs.get(type.getValueType());
                return new MapThriftCodec<>(keyCodec, valueCodec);
              }
              case SET: {
                ThriftCodec<?> elementCodec = typeCodecs.get(type.getValueType());
                return new SetThriftCodec<>(elementCodec);
              }
              case LIST: {
                ThriftCodec<?> elementCodec = typeCodecs.get(type.getValueType());
                return new ListThriftCodec<>(elementCodec);
              }
              case ENUM: {
                // todo implement enums
                throw new UnsupportedOperationException("enums are not implemented");
              }
              default:
                throw new IllegalArgumentException("Unsupported Thrift type " + type);
            }
          }
        }
    );

    for (ThriftCodec<?> codec : codecs) {
      typeCodecs.put(codec.getType(), codec);
    }

    addCodecIfPresent(new BooleanThriftCodec());
    addCodecIfPresent(new ByteThriftCodec());
    addCodecIfPresent(new ShortThriftCodec());
    addCodecIfPresent(new IntegerThriftCodec());
    addCodecIfPresent(new LongThriftCodec());
    addCodecIfPresent(new DoubleThriftCodec());
    addCodecIfPresent(new StringThriftCodec());
    addCodecIfPresent(new ByteBufferThriftCodec());
  }

  public <T> ThriftCodec<T> getCodec(Class<T> type) {
    return (ThriftCodec<T>) getCodec(catalog.getThriftType(type));
  }

  public void addCodec(ThriftCodec<?> codec) {
    typeCodecs.put(codec.getType(), codec);
  }

  public ThriftCatalog getCatalog() {
    return catalog;
  }

  public ThriftCodec<?> getCodec(ThriftType type) {
    try {
      return typeCodecs.get(type);
    } catch (ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public <T> T read(Class<T> type, TProtocol protocol) throws Exception {
    return getCodec(type).read(new TProtocolReader(protocol));
  }

  public Object read(ThriftType type, TProtocol protocol) throws Exception {
    ThriftCodec<?> codec = getCodec(type);
    return codec.read(new TProtocolReader(protocol));
  }

  public <T> void write(Class<T> type, T value, TProtocol protocol) throws Exception {
    getCodec(type).write(value, new TProtocolWriter(protocol));
  }

  public void write(ThriftType type, Object value, TProtocol protocol) throws Exception {
    ThriftCodec<Object> codec = (ThriftCodec<Object>) getCodec(type);
    codec.write(value, new TProtocolWriter(protocol));
  }

  private void addCodecIfPresent(ThriftCodec<?> codec) {
    if (typeCodecs.getIfPresent(codec.getType()) == null) {
      typeCodecs.put(codec.getType(), codec);
    }
  }
}
