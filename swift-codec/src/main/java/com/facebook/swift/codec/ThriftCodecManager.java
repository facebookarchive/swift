/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

import com.facebook.swift.codec.internal.EnumThriftCodec;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.internal.ThriftCodecFactory;
import com.facebook.swift.codec.internal.builtin.BooleanThriftCodec;
import com.facebook.swift.codec.internal.builtin.ByteBufferThriftCodec;
import com.facebook.swift.codec.internal.builtin.ByteThriftCodec;
import com.facebook.swift.codec.internal.builtin.DoubleThriftCodec;
import com.facebook.swift.codec.internal.builtin.IntegerThriftCodec;
import com.facebook.swift.codec.internal.builtin.ListThriftCodec;
import com.facebook.swift.codec.internal.builtin.LongThriftCodec;
import com.facebook.swift.codec.internal.builtin.MapThriftCodec;
import com.facebook.swift.codec.internal.builtin.SetThriftCodec;
import com.facebook.swift.codec.internal.builtin.ShortThriftCodec;
import com.facebook.swift.codec.internal.builtin.VoidThriftCodec;
import com.facebook.swift.codec.internal.coercion.CoercionThriftCodec;
import com.facebook.swift.codec.internal.compiler.CompilerThriftCodecFactory;
import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.codec.metadata.TypeCoercion;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import org.apache.thrift.protocol.TProtocol;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * ThriftCodecManager contains an index of all known ThriftCodec and can create codecs for
 * unknown types as needed.  Since codec creation can be very expensive only one instance of this
 * class should be created.
 */
@ThreadSafe
public class ThriftCodecManager
{
    private final ThriftCatalog catalog;
    private final LoadingCache<ThriftType, ThriftCodec<?>> typeCodecs;

    public ThriftCodecManager(ThriftCodec<?>... codecs)
    {
        this(new CompilerThriftCodecFactory(), codecs);
        for (ThriftCodec<?> codec : codecs) {
            catalog.addThriftType(codec.getType());
        }
    }

    @Inject
    public ThriftCodecManager(@InternalThriftCodec Set<ThriftCodec<?>> codecs)
    {
        this(new CompilerThriftCodecFactory(), codecs);
        for (ThriftCodec<?> codec : codecs) {
            catalog.addThriftType(codec.getType());
        }
    }

    public ThriftCodecManager(ThriftCodecFactory factory, ThriftCodec<?>... codecs)
    {
        this(factory, new ThriftCatalog(), ImmutableSet.copyOf(codecs));
        for (ThriftCodec<?> codec : codecs) {
            catalog.addThriftType(codec.getType());
        }
    }

    public ThriftCodecManager(ThriftCodecFactory factory, Set<ThriftCodec<?>> codecs)
    {
        this(factory, new ThriftCatalog(), codecs);
        for (ThriftCodec<?> codec : codecs) {
            catalog.addThriftType(codec.getType());
        }
    }

    public ThriftCodecManager(final ThriftCodecFactory factory, final ThriftCatalog catalog, Set<ThriftCodec<?>> codecs)
    {
        Preconditions.checkNotNull(factory, "factory is null");
        Preconditions.checkNotNull(catalog, "catalog is null");

        this.catalog = catalog;

        typeCodecs = CacheBuilder.newBuilder().build(new CacheLoader<ThriftType, ThriftCodec<?>>()
        {
            public ThriftCodec<?> load(ThriftType type)
                    throws Exception
            {
                switch (type.getProtocolType()) {
                    case STRUCT: {
                        return factory.generateThriftTypeCodec(ThriftCodecManager.this, type.getStructMetadata());
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
        });

        addCodec(new BooleanThriftCodec());
        addCodec(new ByteThriftCodec());
        addCodec(new ShortThriftCodec());
        addCodec(new IntegerThriftCodec());
        addCodec(new LongThriftCodec());
        addCodec(new DoubleThriftCodec());
        addCodec(new ByteBufferThriftCodec());
        addCodec(new VoidThriftCodec());

        for (ThriftCodec<?> codec : codecs) {
            addCodec(codec);
        }
    }

    public ThriftCodec<?> getCodec(Type javaType)
    {
        ThriftType thriftType = catalog.getThriftType(javaType);
        Preconditions.checkArgument(thriftType != null, "Unsupported java type %s", javaType);
        return getCodec(thriftType);
    }

    public <T> ThriftCodec<T> getCodec(Class<T> javaType)
    {
        ThriftType thriftType = catalog.getThriftType(javaType);
        Preconditions.checkArgument(thriftType != null, "Unsupported java type %s", javaType.getName());
        return (ThriftCodec<T>) getCodec(thriftType);
    }

    public ThriftCodec<?> getCodec(ThriftType type)
    {
        try {
            ThriftCodec<?> thriftCodec = typeCodecs.get(type);
            return thriftCodec;
        }
        catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

    public <T> ThriftCodec<T> getCodec(TypeToken<T> type)
    {
        return (ThriftCodec<T>) getCodec(type.getType());
    }

    /**
     * Adds or replaces the codec associated with the type contained in the codec.  This does not
     * replace any current users of the existing codec associated with the type.
     */
    public void addCodec(ThriftCodec<?> codec)
    {
        typeCodecs.put(codec.getType(), codec);
    }

    public ThriftCatalog getCatalog()
    {
        return catalog;
    }

    public <T> T read(Class<T> type, TProtocol protocol)
            throws Exception
    {
        return getCodec(type).read(new TProtocolReader(protocol));
    }

    public Object read(ThriftType type, TProtocol protocol)
            throws Exception
    {
        ThriftCodec<?> codec = getCodec(type);
        return codec.read(new TProtocolReader(protocol));
    }

    public <T> void write(Class<T> type, T value, TProtocol protocol)
            throws Exception
    {
        getCodec(type).write(value, new TProtocolWriter(protocol));
    }

    public void write(ThriftType type, Object value, TProtocol protocol)
            throws Exception
    {
        ThriftCodec<Object> codec = (ThriftCodec<Object>) getCodec(type);
        codec.write(value, new TProtocolWriter(protocol));
    }
}
