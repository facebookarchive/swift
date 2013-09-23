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

import com.facebook.swift.codec.ThriftField;
import com.google.common.base.Preconditions;

import java.lang.reflect.Type;

import static com.facebook.swift.codec.metadata.FieldKind.THRIFT_FIELD;
import static com.facebook.swift.codec.metadata.ReflectionHelper.resolveFieldType;

class ParameterInjection extends Injection
{
    private final int parameterIndex;
    private final String extractedName;
    private final Type parameterJavaType;
    private final Type thriftStructType;

    ParameterInjection(Type thriftStructType, int parameterIndex, ThriftField annotation, String extractedName, Type parameterJavaType)
    {
        super(annotation, THRIFT_FIELD);
        this.thriftStructType = thriftStructType;
        Preconditions.checkNotNull(parameterJavaType, "parameterJavaType is null");

        this.parameterIndex = parameterIndex;
        this.extractedName = extractedName;
        this.parameterJavaType = parameterJavaType;
        if (void.class.equals(parameterJavaType)) {
            throw new AssertionError();
        }
        Preconditions.checkArgument(getName() != null || extractedName != null, "Parameter must have an explicit name or an extractedName");
    }

    public int getParameterIndex()
    {
        return parameterIndex;
    }

    @Override
    public String extractName()
    {
        return extractedName;
    }

    @Override
    public Type getJavaType()
    {
        return resolveFieldType(thriftStructType, parameterJavaType);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ParameterInjection");
        sb.append("{parameterIndex=").append(parameterIndex);
        sb.append(", extractedName='").append(extractedName).append('\'');
        sb.append(", parameterJavaType=").append(parameterJavaType);
        sb.append('}');
        return sb.toString();
    }
}
