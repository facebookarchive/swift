/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.metadata;

import com.facebook.miffed.ThriftProtocolFieldType;
import com.facebook.miffed.metadata.Problems.Monitor;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.facebook.miffed.metadata.ThriftType.BOOL;
import static com.facebook.miffed.metadata.ThriftType.BYTE;
import static com.facebook.miffed.metadata.ThriftType.DOUBLE;
import static com.facebook.miffed.metadata.ThriftType.I16;
import static com.facebook.miffed.metadata.ThriftType.I32;
import static com.facebook.miffed.metadata.ThriftType.I64;
import static com.facebook.miffed.metadata.ThriftType.STRING;
import static com.facebook.miffed.metadata.ThriftType.list;
import static com.facebook.miffed.metadata.ThriftType.map;
import static com.facebook.miffed.metadata.ThriftType.set;
import static com.facebook.miffed.metadata.ThriftType.struct;
import static com.facebook.miffed.metadata.TypeParameterUtils.getTypeParameters;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

public class ThriftCatalog
{
    private final Problems.Monitor monitor;
    private final Map<Class<?>, ThriftStructMetadata<?>> structs = new HashMap<>();
    private final ThreadLocal<Deque<Class<?>>> stack = new ThreadLocal<Deque<Class<?>>>()
    {
        @Override
        protected Deque<Class<?>> initialValue()
        {
            return new ArrayDeque<>();
        }
    };

    public ThriftCatalog()
    {
        this(Problems.NULL_MONITOR);
    }

    @VisibleForTesting
    public ThriftCatalog(Monitor monitor)
    {
        this.monitor = monitor;
    }

    Monitor getMonitor()
    {
        return monitor;
    }

    public ThriftType getThriftType(Type javaType)
    {
        ThriftProtocolFieldType protocolType = ThriftProtocolFieldType.inferProtocolType(javaType);
        return getThriftType(javaType, protocolType);
    }

    public ThriftType getThriftType(Type javaType, ThriftProtocolFieldType protocolType)
    {
        switch (protocolType) {
            case BOOL:
                return BOOL;
            case BYTE:
                return BYTE;
            case DOUBLE:
                return DOUBLE;
            case I16:
                return I16;
            case I32:
                return I32;
            case I64:
                return I64;
            case STRING:
                return STRING;
            case STRUCT: {
                Class<?> structClass = (Class<?>) javaType;
                ThriftStructMetadata<?> structMetadata = getThriftStructMetadata(structClass);
                return struct(structMetadata);
            }
            case MAP: {
                Type[] types = getTypeParameters(Map.class, javaType);
                Preconditions.checkArgument(types != null && types.length == 2, "Unable to extract Map key and value types from %s", javaType);
                return map(getThriftType(types[0]), getThriftType(types[1]));
            }
            case SET: {
                Type[] types = getTypeParameters(Set.class, javaType);
                Preconditions.checkArgument(types != null && types.length == 1, "Unable to extract Set element type from %s", javaType);
                return set(getThriftType(types[0]));
            }
            case LIST: {
                Type[] types = getTypeParameters(Iterable.class, javaType);
                Preconditions.checkArgument(types != null && types.length == 1, "Unable to extract List element type from %s", javaType);
                return list(getThriftType(types[0]));
            }
            case ENUM: {
                // todo implement enums
                throw new UnsupportedOperationException("enums are not implemented");
            }
            default: {
                throw new IllegalStateException("Write does not support fields of type " + protocolType);
            }
        }
    }

    public <T> ThriftStructMetadata<T> getThriftStructMetadata(Class<T> configClass)
    {
        Deque<Class<?>> stack = this.stack.get();
        if (stack.contains(configClass)) {
            String path = Joiner.on("->").join(transform(concat(stack, ImmutableList.of(configClass)), new Function<Class<?>, Object>()
            {
                @Override
                public Object apply(@Nullable Class<?> input)
                {
                    return input.getName();
                }
            }));
            throw new IllegalArgumentException("Circular references are not allowed: " + path);
        }

        stack.push(configClass);
        try {
            ThriftStructMetadata<T> structMetadata = (ThriftStructMetadata<T>) structs.get(configClass);
            if (structMetadata == null) {
                ThriftStructMetadataBuilder<T> builder = new ThriftStructMetadataBuilder<>(this, configClass);
                structMetadata = builder.build();
                structs.put(configClass, structMetadata);
            }
            return structMetadata;
        }
        finally {
            Class<?> top = stack.pop();
            Preconditions.checkState(configClass.equals(top), "ThriftCatalog circularity detection stack is corrupt: expected %s, but got %s", configClass, top);
        }
    }
}
