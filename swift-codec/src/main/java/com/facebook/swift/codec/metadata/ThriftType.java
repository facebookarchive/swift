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
package com.facebook.swift.codec.metadata;

import com.facebook.swift.codec.ThriftProtocolType;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import javax.annotation.concurrent.Immutable;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.facebook.swift.codec.ThriftProtocolType.ENUM;
import static com.facebook.swift.codec.ThriftProtocolType.STRUCT;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * ThriftType contains all metadata necessary for converting the java type to and from Thrift.
 */
@Immutable
public class ThriftType
{
    public static final ThriftType BOOL = new ThriftType(ThriftProtocolType.BOOL, boolean.class);
    public static final ThriftType BYTE = new ThriftType(ThriftProtocolType.BYTE, byte.class);
    public static final ThriftType DOUBLE = new ThriftType(ThriftProtocolType.DOUBLE, double.class);
    public static final ThriftType I16 = new ThriftType(ThriftProtocolType.I16, short.class);
    public static final ThriftType I32 = new ThriftType(ThriftProtocolType.I32, int.class);
    public static final ThriftType I64 = new ThriftType(ThriftProtocolType.I64, long.class);
    public static final ThriftType STRING = new ThriftType(ThriftProtocolType.STRING, String.class);
    public static final ThriftType BINARY = new ThriftType(ThriftProtocolType.BINARY, ByteBuffer.class);
    public static final ThriftType VOID = new ThriftType(ThriftProtocolType.STRUCT, void.class);

    public static final ThriftTypeReference BOOL_REF = new DefaultThriftTypeReference(BOOL);
    public static final ThriftTypeReference BYTE_REF = new DefaultThriftTypeReference(BYTE);
    public static final ThriftTypeReference DOUBLE_REF = new DefaultThriftTypeReference(DOUBLE);
    public static final ThriftTypeReference I16_REF = new DefaultThriftTypeReference(I16);
    public static final ThriftTypeReference I32_REF = new DefaultThriftTypeReference(I32);
    public static final ThriftTypeReference I64_REF = new DefaultThriftTypeReference(I64);
    public static final ThriftTypeReference STRING_REF = new DefaultThriftTypeReference(STRING);
    public static final ThriftTypeReference BINARY_REF = new DefaultThriftTypeReference(BINARY);
    public static final ThriftTypeReference VOID_REF = new DefaultThriftTypeReference(VOID);

    public static ThriftType struct(ThriftStructMetadata structMetadata)
    {
        return new ThriftType(structMetadata);
    }

    public static <K, V> ThriftType map(ThriftType keyType, ThriftType valueType)
    {
        checkNotNull(keyType, "keyType is null");
        checkNotNull(valueType, "valueType is null");

        return map(new DefaultThriftTypeReference(keyType), new DefaultThriftTypeReference(valueType));
    }

    public static <K, V> ThriftType map(ThriftTypeReference keyTypeReference,
                                        ThriftTypeReference valueTypeReference)
    {
        checkNotNull(keyTypeReference, "keyTypeReference is null");
        checkNotNull(valueTypeReference, "valueTypeReference is null");

        @SuppressWarnings("serial")
        Type javaType = new TypeToken<Map<K, V>>(){}
                .where(new TypeParameter<K>(){}, (TypeToken<K>) TypeToken.of(keyTypeReference.getJavaType()))
                .where(new TypeParameter<V>(){}, (TypeToken<V>) TypeToken.of(valueTypeReference.getJavaType()))
                .getType();
        return new ThriftType(ThriftProtocolType.MAP, javaType, keyTypeReference, valueTypeReference);
    }

    public static <E> ThriftType set(ThriftType valueType)
    {
        Preconditions.checkNotNull(valueType, "valueType is null");

        return set(new DefaultThriftTypeReference(valueType));
    }

    public static <E> ThriftType set(ThriftTypeReference valueTypeReference)
    {
        Preconditions.checkNotNull(valueTypeReference, "valueTypeReference is null");

        @SuppressWarnings("serial")
        Type javaType = new TypeToken<Set<E>>(){}
                .where(new TypeParameter<E>(){}, (TypeToken<E>) TypeToken.of(valueTypeReference.getJavaType()))
                .getType();
        return new ThriftType(ThriftProtocolType.SET, javaType, null, valueTypeReference);
    }

    public static <E> ThriftType list(ThriftType valueType)
    {
        checkNotNull(valueType, "valueType is null");

        return list(new DefaultThriftTypeReference(valueType));
    }

    public static <E> ThriftType list(ThriftTypeReference valueTypeReference)
    {
        checkNotNull(valueTypeReference, "valueTypeReference is null");

        @SuppressWarnings("serial")
        Type javaType = new TypeToken<List<E>>(){}
                .where(new TypeParameter<E>(){}, (TypeToken<E>) TypeToken.of(valueTypeReference.getJavaType()))
                .getType();
        return new ThriftType(ThriftProtocolType.LIST, javaType, null, valueTypeReference);
    }

    public static ThriftType array(ThriftType valueType)
    {
        checkNotNull(valueType, "valueType is null");

        return array(new DefaultThriftTypeReference(valueType));
    }

    public static ThriftType array(ThriftTypeReference valueTypeReference)
    {
        checkNotNull(valueTypeReference, "valueTypeReference is null");

        Class<?> javaType = ReflectionHelper.getArrayOfType(valueTypeReference.getJavaType());
        return new ThriftType(ThriftProtocolType.LIST, javaType, null, valueTypeReference);
    }

    public static ThriftType enumType(ThriftEnumMetadata<?> enumMetadata)
    {
        checkNotNull(enumMetadata, "enumMetadata is null");
        return new ThriftType(enumMetadata);
    }

    private final ThriftProtocolType protocolType;
    private final Type javaType;
    private final ThriftTypeReference keyTypeReference;
    private final ThriftTypeReference valueTypeReference;
    private final ThriftStructMetadata structMetadata;
    private final ThriftEnumMetadata<?> enumMetadata;
    private final ThriftType uncoercedType;

    private ThriftType(ThriftProtocolType protocolType, Type javaType)
    {
        Preconditions.checkNotNull(protocolType, "protocolType is null");
        Preconditions.checkNotNull(javaType, "javaType is null");

        this.protocolType = protocolType;
        this.javaType = javaType;
        keyTypeReference = null;
        valueTypeReference = null;
        structMetadata = null;
        enumMetadata = null;
        uncoercedType = null;
    }

    private ThriftType(ThriftProtocolType protocolType,
                       Type javaType,
                       ThriftTypeReference keyTypeReference,
                       ThriftTypeReference valueTypeReference)
    {
        Preconditions.checkNotNull(protocolType, "protocolType is null");
        Preconditions.checkNotNull(javaType, "javaType is null");
        Preconditions.checkNotNull(valueTypeReference, "valueTypeReference is null");

        this.protocolType = protocolType;
        this.javaType = javaType;
        this.keyTypeReference = keyTypeReference;
        this.valueTypeReference = valueTypeReference;
        this.structMetadata = null;
        this.enumMetadata = null;
        this.uncoercedType = null;
    }

    private ThriftType(ThriftStructMetadata structMetadata)
    {
        Preconditions.checkNotNull(structMetadata, "structMetadata is null");

        this.protocolType = STRUCT;
        this.javaType = structMetadata.getStructClass();
        keyTypeReference = null;
        valueTypeReference = null;
        this.structMetadata = structMetadata;
        this.enumMetadata = null;
        this.uncoercedType = null;
    }

    private ThriftType(ThriftEnumMetadata<?> enumMetadata)
    {
        Preconditions.checkNotNull(enumMetadata, "enumMetadata is null");

        this.protocolType = ENUM;
        this.javaType = enumMetadata.getEnumClass();
        keyTypeReference = null;
        valueTypeReference = null;
        this.structMetadata = null;
        this.enumMetadata = enumMetadata;
        this.uncoercedType = null;
    }

    public ThriftType(ThriftType uncoercedType, Type javaType)
    {
        this.javaType = javaType;
        this.uncoercedType = uncoercedType;

        this.protocolType = uncoercedType.getProtocolType();
        keyTypeReference = null;
        valueTypeReference = null;
        structMetadata = null;
        enumMetadata = null;
    }

    public Type getJavaType()
    {
        return javaType;
    }

    public ThriftProtocolType getProtocolType()
    {
        return protocolType;
    }

    public ThriftTypeReference getKeyTypeReference()
    {
        checkState(keyTypeReference != null, "%s does not have a key", protocolType);
        return keyTypeReference;
    }

    public ThriftTypeReference getValueTypeReference()
    {
        checkState(valueTypeReference != null, "%s does not have a get", protocolType);
        return valueTypeReference;
    }

    public ThriftStructMetadata getStructMetadata()
    {
        checkState(structMetadata != null, "%s does not have struct metadata", protocolType);
        return structMetadata;
    }

    public ThriftEnumMetadata<?> getEnumMetadata()
    {
        checkState(enumMetadata != null, "%s does not have enum metadata", protocolType);
        return enumMetadata;
    }

    public boolean isCoerced()
    {
        return uncoercedType != null;
    }

    public ThriftType coerceTo(Type javaType)
    {
        if (javaType == this.javaType) {
            return this;
        }

        Preconditions.checkState(
                protocolType != ThriftProtocolType.STRUCT &&
                protocolType != ThriftProtocolType.SET &&
                protocolType != ThriftProtocolType.LIST &&
                protocolType != ThriftProtocolType.MAP,
                "Coercion is not supported for %s", protocolType
        );
        return new ThriftType(this, javaType);
    }

    public ThriftType getUncoercedType()
    {
        return uncoercedType;
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

        final ThriftType that = (ThriftType) o;

        if (javaType != null ? !javaType.equals(that.javaType) : that.javaType != null) {
            return false;
        }
        if (protocolType != that.protocolType) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = protocolType != null ? protocolType.hashCode() : 0;
        result = 31 * result + (javaType != null ? javaType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftType");
        sb.append("{");
        sb.append(protocolType).append(" ").append(javaType);
        if (structMetadata != null) {
            sb.append(" ").append(getStructMetadata().getStructClass().getName());
        }
        else if (keyTypeReference != null) {
            sb.append(" keyTypeReference=").append(keyTypeReference);
            sb.append(", valueTypeReference=").append(valueTypeReference);
        }
        else if (valueTypeReference != null) {
            sb.append(" valueTypeReference=").append(valueTypeReference);
        }
        sb.append('}');
        return sb.toString();
    }
}
