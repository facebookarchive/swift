/**
 * Copyright 2012 Facebook, Inc.
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
package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class BaseType
        extends ThriftType
{
    public static enum Type
    {
        BOOL, BYTE, I16, I32, I64, DOUBLE, STRING, BINARY
    }

    private final Type type;
    private final List<TypeAnnotation> annotations;

    private static final Map<Type, Class> typeToJavaType =
            ImmutableMap.<Type, Class>builder()
                        .put(Type.BOOL, boolean.class)
                        .put(Type.BYTE, byte.class)
                        .put(Type.I16, short.class)
                        .put(Type.I32, int.class)
                        .put(Type.I64, long.class)
                        .put(Type.DOUBLE, double.class)
                        .put(Type.STRING, String.class)
                        .put(Type.BINARY, ByteBuffer.class)
                        .build();

    public BaseType(Type type, List<TypeAnnotation> annotations)
    {
        this.type = checkNotNull(type, "type");
        this.annotations = ImmutableList.copyOf(checkNotNull(annotations, "annotations"));
    }

    public static String getJavaTypeName(Type type)
    {
        Class primitiveType = typeToJavaType.get(type);
        return Primitives.wrap(primitiveType).getSimpleName();
    }

    public Type getType()
    {
        return type;
    }

    public List<TypeAnnotation> getAnnotations()
    {
        return annotations;
    }

    @Override
    public TypeToken getJavaTypeToken()
    {
        return TypeToken.of(typeToJavaType.get(type));
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("type", type)
                .add("annotations", annotations)
                .toString();
    }
}
