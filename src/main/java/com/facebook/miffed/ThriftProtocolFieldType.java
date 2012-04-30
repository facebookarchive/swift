/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.facebook.miffed.metadata.TypeParameterUtils.getRawType;

public enum ThriftProtocolFieldType
{
    STOP(false),
    VOID(false),
    BOOL(boolean.class, Boolean.class),
    BYTE(byte.class, Byte.class),
    DOUBLE(double.class, Double.class),
    $_5_IS_SKIPPED(false),
    I16(short.class, Short.class),
    $_7_IS_SKIPPED(false),
    I32(int.class, Integer.class),
    $_9_IS_SKIPPED(false),
    I64(long.class, Long.class),
    STRING(String.class),
    STRUCT,
    MAP,
    SET,
    LIST,
    ENUM;


    private final boolean validFieldType;
    private final List<Class<?>> basicTypes;

    private ThriftProtocolFieldType(Class<?>... types)
    {
        this.validFieldType = true;
        basicTypes = ImmutableList.copyOf(types);
    }

    private ThriftProtocolFieldType(boolean validFieldType)
    {
        this.validFieldType = validFieldType;
        basicTypes = ImmutableList.of();
    }

    public boolean isValidFieldType()
    {
        return validFieldType;
    }

    public byte getType()
    {
        return (byte) ordinal();
    }

    private static Map<Class<?>, ThriftProtocolFieldType> typesByClass;
    static {
        ImmutableMap.Builder<Class<?>, ThriftProtocolFieldType> builder = ImmutableMap.builder();
        for (ThriftProtocolFieldType protocolType : ThriftProtocolFieldType.values()) {
            for (Class<?> basicType : protocolType.basicTypes) {
                builder.put(basicType, protocolType);
            }
        }
        typesByClass = builder.build();
    }
    
    public static ThriftProtocolFieldType inferProtocolType(Type genericType)
    {
        Class<?> rawType = getRawType(genericType);

        ThriftProtocolFieldType protocolType = typesByClass.get(rawType);
        if (protocolType != null) {
            return protocolType;
        }
        if (Map.class.isAssignableFrom(rawType)) {
            return MAP;
        }
        if (Set.class.isAssignableFrom(rawType)) {
            return SET;
        }
        if (Iterable.class.isAssignableFrom(rawType)) {
            return LIST;
        }
        if (rawType.isAnnotationPresent(ThriftStruct.class)) {
            return STRUCT;
        }

        throw new IllegalArgumentException(String.format("Unsupported type %s: type must be " +
                "a primitive, " +
                "boxed wrapper, " +
                "String, " +
                "Iterable, " +
                "Map or " +
                "must be annotated with @ThriftStruct",
                genericType));
    }
}
