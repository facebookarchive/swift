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

import javax.annotation.concurrent.Immutable;

import java.lang.reflect.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class ThriftParameterInjection implements ThriftInjection
{
    private final short id;
    private final String name;
    private final int parameterIndex;
    private final Type javaType;

    public ThriftParameterInjection(
            short id,
            String name,
            int parameterIndex,
            Type javaType)
    {

        checkArgument(parameterIndex >= 0, "parameterIndex is negative");

        this.javaType = checkNotNull(javaType, "javaType is null");
        this.name = checkNotNull(name, "name is null");

        this.id = id;
        this.parameterIndex = parameterIndex;
    }

    @Override
    public short getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public FieldKind getFieldKind()
    {
        return FieldKind.THRIFT_FIELD;
    }

    public int getParameterIndex()
    {
        return parameterIndex;
    }

    public Type getJavaType()
    {
        return javaType;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftParameterInjection");
        sb.append("{fieldId=").append(id);
        sb.append(", name=").append(name);
        sb.append(", index=").append(parameterIndex);
        sb.append(", javaType=").append(javaType);
        sb.append('}');
        return sb.toString();
    }
}
