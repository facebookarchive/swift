/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.guice;

import com.facebook.swift.codec.InternalThriftCodec;
import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.MoreTypes.ParameterizedTypeImpl;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

public class ThriftCodecBinder
{
    public static ThriftCodecBinder thriftServerBinder(Binder binder)
    {
        return new ThriftCodecBinder(binder);
    }

    private final Binder binder;

    private ThriftCodecBinder(Binder binder)
    {
        this.binder = binder;
    }

    public void bindThriftCodec(ThriftCodec<?> thriftCodec)
    {
        Preconditions.checkNotNull(thriftCodec, "thriftCodec is null");

        // bind the instance to the internal thrift codec set
        newSetBinder(binder, new TypeLiteral<ThriftCodec<?>>() {}, InternalThriftCodec.class).addBinding().toInstance(thriftCodec);

        // make the codec available to user code for binding
        Type type = thriftCodec.getType().getJavaType();
        binder.bind(getThriftCodecKey(type)).toProvider(new ThriftCodecProvider(type)).in(Scopes.SINGLETON);
    }

    public void bindThriftCodec(Class<?> type)
    {
        Preconditions.checkNotNull(type, "type is null");

        binder.bind(getThriftCodecKey(type)).toProvider(new ThriftCodecProvider(type)).in(Scopes.SINGLETON);
    }

    public void bindThriftCodec(TypeLiteral<?> type)
    {
        Preconditions.checkNotNull(type, "type is null");

        binder.bind(getThriftCodecKey(type.getType())).toProvider(new ThriftCodecProvider(type.getType())).in(Scopes.SINGLETON);
    }

    public void bindListThriftCodec(Class<?> type)
    {
        Preconditions.checkNotNull(type, "type is null");

        ParameterizedTypeImpl listType = new ParameterizedTypeImpl(null, List.class, type);
        binder.bind(getThriftCodecKey(listType)).toProvider(new ThriftCodecProvider(listType)).in(Scopes.SINGLETON);
    }

    public void bindMapThriftCodec(Class<?> keyType, Class<?> valueType)
    {
        Preconditions.checkNotNull(keyType, "keyType is null");
        Preconditions.checkNotNull(valueType, "valueType is null");

        ParameterizedTypeImpl mapType = new ParameterizedTypeImpl(null, Map.class, keyType, valueType);
        binder.bind(getThriftCodecKey(mapType)).toProvider(new ThriftCodecProvider(mapType)).in(Scopes.SINGLETON);
    }

    private Key<ThriftCodec<?>> getThriftCodecKey(Type type)
    {
        return (Key<ThriftCodec<?>>) Key.get(new ParameterizedTypeImpl(null, ThriftCodec.class, type));
    }

    class ThriftCodecProvider implements Provider<ThriftCodec<?>>
    {
        private final Type type;
        private ThriftCodecManager thriftCodecManager;

        public ThriftCodecProvider(Type type)
        {
            this.type = type;
        }

        @Inject
        public void setThriftCodecManager(ThriftCodecManager thriftCodecManager)
        {
            this.thriftCodecManager = thriftCodecManager;
        }

        @Override
        public ThriftCodec<?> get()
        {
            return thriftCodecManager.getCodec(type);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ThriftCodecProvider that = (ThriftCodecProvider) o;

            if (!type.equals(that.type)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            return type.hashCode();
        }
    }
}

