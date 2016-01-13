/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.codec;

import com.facebook.swift.codec.internal.EnumThriftCodec;
import com.facebook.swift.codec.internal.ThriftCodecFactory;
import com.facebook.swift.codec.internal.builtin.BooleanArrayThriftCodec;
import com.facebook.swift.codec.internal.builtin.BooleanThriftCodec;
import com.facebook.swift.codec.internal.builtin.ByteBufferThriftCodec;
import com.facebook.swift.codec.internal.builtin.ByteThriftCodec;
import com.facebook.swift.codec.internal.builtin.DoubleArrayThriftCodec;
import com.facebook.swift.codec.internal.builtin.DoubleThriftCodec;
import com.facebook.swift.codec.internal.builtin.IntArrayThriftCodec;
import com.facebook.swift.codec.internal.builtin.IntegerThriftCodec;
import com.facebook.swift.codec.internal.builtin.ListThriftCodec;
import com.facebook.swift.codec.internal.builtin.LongArrayThriftCodec;
import com.facebook.swift.codec.internal.builtin.LongThriftCodec;
import com.facebook.swift.codec.internal.builtin.MapThriftCodec;
import com.facebook.swift.codec.internal.builtin.SetThriftCodec;
import com.facebook.swift.codec.internal.builtin.ShortArrayThriftCodec;
import com.facebook.swift.codec.internal.builtin.ShortThriftCodec;
import com.facebook.swift.codec.internal.builtin.StringThriftCodec;
import com.facebook.swift.codec.internal.builtin.VoidThriftCodec;
import com.facebook.swift.codec.internal.coercion.CoercionThriftCodec;
import com.facebook.swift.codec.internal.compiler.CompilerThriftCodecFactory;
import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.codec.metadata.ThriftTypeReference;
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
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TIOStreamTransport;

import javax.annotation.concurrent.ThreadSafe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkState;

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

    /**
     * This stack tracks the java Types for which building a ThriftCodec is in progress (used to
     * detect recursion)
     */
    private final ThreadLocal<Deque<ThriftType>> stack = new ThreadLocal<Deque<ThriftType>>()
    {
        @Override
        protected Deque<ThriftType> initialValue()
        {
            return new ArrayDeque<>();
        }
    };

    /**
     * Tracks the Types for which building a ThriftCodec was deferred to allow for recursive type
     * structures. These will be handled immediately after the originally requested ThriftCodec is
     * built and cached.
     */
    private final ThreadLocal<Deque<ThriftType>> deferredTypesWorkList = new ThreadLocal<Deque<ThriftType>>()
    {
        @Override
        protected Deque<ThriftType> initialValue()
        {
            return new ArrayDeque<>();
        }
    };

    public ThriftCodecManager(ThriftCodec<?>... codecs)
    {
        this(new CompilerThriftCodecFactory(ThriftCodecManager.class.getClassLoader()), ImmutableSet.copyOf(codecs));
    }

    public ThriftCodecManager(ClassLoader parent, ThriftCodec<?>... codecs)
    {
        this(new CompilerThriftCodecFactory(parent), ImmutableSet.copyOf(codecs));
    }

    public ThriftCodecManager(ThriftCodecFactory factory, ThriftCodec<?>... codecs)
    {
        this(factory, new ThriftCatalog(), ImmutableSet.copyOf(codecs));
    }

    public ThriftCodecManager(ThriftCodecFactory factory, Set<ThriftCodec<?>> codecs)
    {
        this(factory, new ThriftCatalog(), codecs);
    }

    @Inject
    public ThriftCodecManager(final ThriftCodecFactory factory, final ThriftCatalog catalog, @InternalThriftCodec Set<ThriftCodec<?>> codecs)
    {
        Preconditions.checkNotNull(factory, "factory is null");
        Preconditions.checkNotNull(catalog, "catalog is null");

        this.catalog = catalog;

        typeCodecs = CacheBuilder.newBuilder().build(new CacheLoader<ThriftType, ThriftCodec<?>>()
        {
            public ThriftCodec<?> load(ThriftType type)
                    throws Exception
            {
                try {
                    // When we need to load a codec for a type the first time, we push it on the
                    // thread-local stack before starting the load, and pop it off afterwards,
                    // so that we can detect recursive loads.
                    stack.get().push(type);

                    switch (type.getProtocolType()) {
                        case STRUCT: {
                            return factory.generateThriftTypeCodec(ThriftCodecManager.this, type.getStructMetadata());
                        }
                        case MAP: {
                            return new MapThriftCodec<>(type, getElementCodec(type.getKeyTypeReference()), getElementCodec(type.getValueTypeReference()));
                        }
                        case SET: {
                            return new SetThriftCodec<>(type, getElementCodec(type.getValueTypeReference()));
                        }
                        case LIST: {
                            return new ListThriftCodec<>(type, getElementCodec(type.getValueTypeReference()));
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
                finally {
                    ThriftType top = stack.get().pop();
                    checkState(type.equals(top),
                               "ThriftCatalog circularity detection stack is corrupt: expected %s, but got %s",
                               type,
                               top);
                }

            }
        });

        addBuiltinCodec(new BooleanThriftCodec());
        addBuiltinCodec(new ByteThriftCodec());
        addBuiltinCodec(new ShortThriftCodec());
        addBuiltinCodec(new IntegerThriftCodec());
        addBuiltinCodec(new LongThriftCodec());
        addBuiltinCodec(new DoubleThriftCodec());
        addBuiltinCodec(new ByteBufferThriftCodec());
        addBuiltinCodec(new StringThriftCodec());
        addBuiltinCodec(new VoidThriftCodec());
        addBuiltinCodec(new BooleanArrayThriftCodec());
        addBuiltinCodec(new ShortArrayThriftCodec());
        addBuiltinCodec(new IntArrayThriftCodec());
        addBuiltinCodec(new LongArrayThriftCodec());
        addBuiltinCodec(new DoubleArrayThriftCodec());

        for (ThriftCodec<?> codec : codecs) {
            addCodec(codec);
        }
    }

    public ThriftCodec<?> getElementCodec(ThriftTypeReference thriftTypeReference)
            throws Exception
    {
        return getCodec(thriftTypeReference.get());
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

    public <T> ThriftCodec<T> getCodec(TypeToken<T> type)
    {
        return (ThriftCodec<T>) getCodec(type.getType());
    }

    public ThriftCodec<?> getCodec(ThriftType type)
    {
        // The loading function pushes types before they are loaded and pops them afterwards in
        // order to detect recursive loading (which will would otherwise fail in the LoadingCache).
        // In this case, to avoid the cycle, we return a DelegateCodec that points back to this
        // ThriftCodecManager and references the type. When used, the DelegateCodec will require
        // that our cache contain an actual ThriftCodec, but this should not be a problem as
        // it won't be used while we are loading types, and by the time we're done loading the
        // type at the top of the stack, *all* types on the stack should have been loaded and
        // cached.
        if (stack.get().contains(type)) {
            return new DelegateCodec(this, type.getJavaType());
        }

        try {
            ThriftCodec<?> thriftCodec = typeCodecs.get(type);

            while (!deferredTypesWorkList.get().isEmpty()) {
                getCodec(deferredTypesWorkList.get().pop());
            }

            return thriftCodec;
        }
        catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

    public ThriftCodec<?> getCachedCodecIfPresent(Type javaType)
    {
        ThriftType thriftType = catalog.getThriftType(javaType);
        Preconditions.checkArgument(thriftType != null, "Unsupported java type %s", javaType);
        return getCachedCodecIfPresent(thriftType);
    }

    public <T> ThriftCodec<T> getCachedCodecIfPresent(Class<T> javaType)
    {
        ThriftType thriftType = catalog.getThriftType(javaType);
        Preconditions.checkArgument(thriftType != null, "Unsupported java type %s", javaType.getName());
        return (ThriftCodec<T>) getCachedCodecIfPresent(thriftType);
    }

    public <T> ThriftCodec<T> getCachedCodecIfPresent(TypeToken<T> type)
    {
        return (ThriftCodec<T>) getCachedCodecIfPresent(type.getType());
    }

    public ThriftCodec<?> getCachedCodecIfPresent(ThriftType type)
    {
        ThriftCodec<?> thriftCodec = typeCodecs.getIfPresent(type);
        return thriftCodec;
    }

    /**
     * Adds or replaces the codec associated with the type contained in the codec.  This does not
     * replace any current users of the existing codec associated with the type.
     */
    public void addCodec(ThriftCodec<?> codec)
    {
        catalog.addThriftType(codec.getType());
        typeCodecs.put(codec.getType(), codec);
    }

    /**
     * Adds a ThriftCodec to the codec map, but does not register it with the catalog since builtins
     * should already be registered
     */
    private void addBuiltinCodec(ThriftCodec<?> codec)
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
        return getCodec(type).read(protocol);
    }

    public Object read(ThriftType type, TProtocol protocol)
            throws Exception
    {
        ThriftCodec<?> codec = getCodec(type);
        return codec.read(protocol);
    }

    public <T> T read(byte[] serializedStruct,
                      Class<T> clazz,
                      TProtocolFactory protocolFactory) {
        Preconditions.checkNotNull(serializedStruct, "ttype is null");
        Preconditions.checkNotNull(clazz, "clazz is null");
        try {
            ByteArrayInputStream istream = new ByteArrayInputStream(serializedStruct);
            TIOStreamTransport resultIOStream = new TIOStreamTransport(istream);
            TProtocol resultProtocolBuffer = protocolFactory.getProtocol(resultIOStream);
            return read(clazz, resultProtocolBuffer);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public <T> void write(Class<T> type, T value, TProtocol protocol)
            throws Exception
    {
        getCodec(type).write(value, protocol);
    }

    public <T> void write(T ttype,
                          ByteArrayOutputStream oStream,
                          TProtocolFactory protocolFactory) {
        Preconditions.checkNotNull(ttype, "ttype is null");
        Preconditions.checkNotNull(protocolFactory, "protocolFactory is null");
        try {
            TIOStreamTransport resultIOStream = new TIOStreamTransport(oStream);
            TProtocol resultProtocolBuffer = protocolFactory.getProtocol(resultIOStream);
            write((Class<T>) ttype.getClass(), ttype, resultProtocolBuffer);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void write(ThriftType type, Object value, TProtocol protocol)
            throws Exception
    {
        ThriftCodec<Object> codec = (ThriftCodec<Object>) getCodec(type);
        codec.write(value, protocol);
    }
}
