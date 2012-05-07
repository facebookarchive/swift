/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

import com.facebook.swift.codec.builtin.BooleanThriftCodec;
import com.facebook.swift.codec.builtin.ByteBufferThriftCodec;
import com.facebook.swift.codec.builtin.ByteThriftCodec;
import com.facebook.swift.codec.builtin.VoidThriftCodec;
import com.facebook.swift.codec.coercion.CoercionThriftCodec;
import com.facebook.swift.codec.internal.EnumThriftCodec;
import com.facebook.swift.codec.internal.compiler.CompilerThriftCodecFactory;
import com.facebook.swift.codec.builtin.DoubleThriftCodec;
import com.facebook.swift.codec.builtin.IntegerThriftCodec;
import com.facebook.swift.codec.builtin.ListThriftCodec;
import com.facebook.swift.codec.builtin.LongThriftCodec;
import com.facebook.swift.codec.builtin.MapThriftCodec;
import com.facebook.swift.codec.builtin.SetThriftCodec;
import com.facebook.swift.codec.builtin.ShortThriftCodec;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.internal.ThriftCodecFactory;
import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.codec.metadata.TypeCoercion;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.thrift.protocol.TProtocol;

import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;

/**
 * ThriftCodecManager contains an index of all known ThriftCodec and can create codecs for
 * unknown types as needed.  Since codec creation can be very expensive only one instance of this
 * class should be created.
 */
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
      final ThriftCatalog catalog,
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
                return new MapThriftCodec<>(type, keyCodec, valueCodec);
              }
              case SET: {
                ThriftCodec<?> elementCodec = typeCodecs.get(type.getValueType());
                return new SetThriftCodec<>(type, elementCodec);
              }
              case LIST: {
                ThriftCodec<?> elementCodec = typeCodecs.get(type.getValueType());
                return new ListThriftCodec<>(type, elementCodec);
              }
              case ENUM: {
                return new EnumThriftCodec<>(type);
              }
              default:
                if (type.isCoerced()) {
                  ThriftCodec<?> codec = getCodec(type.getUncoercedType());
                  TypeCoercion coercion = catalog.getDefaultCoercion(type.getJavaType());
                  return new CoercionThriftCodec<>(codec, coercion);
                }
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
    addCodecIfPresent(new ByteBufferThriftCodec());
    addCodecIfPresent(new VoidThriftCodec());
  }

  public ThriftCodec<?> getCodec(Type javaType) {
    ThriftType thriftType = catalog.getThriftType(javaType);
    Preconditions.checkArgument(thriftType != null, "Unsupported java type %s", javaType);
    return getCodec(thriftType);
  }

  public <T> ThriftCodec<T> getCodec(Class<T> javaType) {
    ThriftType thriftType = catalog.getThriftType(javaType);
    Preconditions.checkArgument(thriftType != null, "Unsupported java type %s", javaType.getName());
    return (ThriftCodec<T>) getCodec(thriftType);
  }

  public ThriftCodec<?> getCodec(ThriftType type) {
    try {
      ThriftCodec<?> thriftCodec = typeCodecs.get(type);
      return thriftCodec;
    } catch (ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Adds or replaces the codec associated with the type contained in the codec.  This does not
   * replace any current users of the existing codec associated with the type.
   */
  public void addCodec(ThriftCodec<?> codec) {
    typeCodecs.put(codec.getType(), codec);
  }

  public ThriftCatalog getCatalog() {
    return catalog;
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
