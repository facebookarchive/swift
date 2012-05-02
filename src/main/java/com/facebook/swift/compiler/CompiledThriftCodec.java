/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftCatalog;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.thrift.protocol.TProtocol;

import java.util.concurrent.ExecutionException;

public class CompiledThriftCodec implements ThriftCodec {
  private final ThriftCatalog catalog;
  private final LoadingCache<ThriftType, ThriftTypeCodec<?>> typeCodecs;
  private final ThriftCodecCompiler compiler;

  public CompiledThriftCodec(ThriftCatalog catalog, ThriftTypeCodec<?>... codecs) {
    this(catalog, new DynamicClassLoader(), codecs);
  }

  public CompiledThriftCodec(
      ThriftCatalog catalog,
      DynamicClassLoader classLoader,
      ThriftTypeCodec<?>... codecs
  ) {
    this.catalog = catalog;
    this.compiler = new ThriftCodecCompiler(this, classLoader);

    typeCodecs = CacheBuilder.newBuilder().build(
        new CacheLoader<ThriftType, ThriftTypeCodec<?>>() {
          public ThriftTypeCodec<?> load(ThriftType type) throws Exception {
            switch (type.getProtocolType()) {
              case STRUCT: {
                return compiler.generateThriftTypeCodec(type.getStructMetadata().getStructClass());
              }
              case MAP: {
                ThriftTypeCodec<?> keyCodec = typeCodecs.get(type.getKeyType());
                ThriftTypeCodec<?> valueCodec = typeCodecs.get(type.getValueType());
                return new MapThriftTypeCodec<>(keyCodec, valueCodec);
              }
              case SET: {
                ThriftTypeCodec<?> elementCodec = typeCodecs.get(type.getValueType());
                return new SetThriftTypeCodec<>(elementCodec);
              }
              case LIST: {
                ThriftTypeCodec<?> elementCodec = typeCodecs.get(type.getValueType());
                return new ListThriftTypeCodec<>(elementCodec);
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

    for (ThriftTypeCodec<?> codec : codecs) {
      typeCodecs.put(codec.getType(), codec);
    }

    addCodecIfPresent(new BooleanThriftTypeCodec());
    addCodecIfPresent(new ByteThriftTypeCodec());
    addCodecIfPresent(new ShortThriftTypeCodec());
    addCodecIfPresent(new IntegerThriftTypeCodec());
    addCodecIfPresent(new LongThriftTypeCodec());
    addCodecIfPresent(new DoubleThriftTypeCodec());
    addCodecIfPresent(new StringThriftTypeCodec());
    addCodecIfPresent(new ByteBufferThriftTypeCodec());
  }

  private void addCodecIfPresent(ThriftTypeCodec<?> codec) {
    if (typeCodecs.getIfPresent(codec.getType()) == null) {
      typeCodecs.put(codec.getType(), codec);
    }
  }

  public ThriftCatalog getCatalog() {
    return catalog;
  }

  public <T> ThriftTypeCodec<T> getCodec(Class<T> type) {
    return (ThriftTypeCodec<T>) getCodec(catalog.getThriftType(type));
  }

  @Override
  public <T> T read(Class<T> type, TProtocol protocol) throws Exception {
    return getCodec(type).read(new TProtocolReader(protocol));
  }

  @Override
  public <T> void write(Class<T> type, T value, TProtocol protocol) throws Exception {
    getCodec(type).write(value, new TProtocolWriter(protocol));
  }

  public ThriftTypeCodec<?> getCodec(ThriftType type) {
    try {
      return typeCodecs.get(type);
    } catch (ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public Object read(ThriftType type, TProtocol protocol) throws Exception {
    ThriftTypeCodec<?> codec = getCodec(type);
    return codec.read(new TProtocolReader(protocol));
  }

  @Override
  public void write(ThriftType type, Object value, TProtocol protocol) throws Exception {
    ThriftTypeCodec<Object> codec = (ThriftTypeCodec<Object>) getCodec(type);
    codec.write(value, new TProtocolWriter(protocol));
  }
}
