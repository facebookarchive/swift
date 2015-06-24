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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.google.common.reflect.TypeToken;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class ThriftMethodExtractor implements ThriftExtraction
{
    private final short id;
    private final String name;
    private final Method method;
    private final FieldKind fieldKind;
    private final Class<?> type;

    public ThriftMethodExtractor(
            short fieldId, String fieldName, FieldKind fieldKind, Method method, Type fieldType)
    {
        this.name = checkNotNull(fieldName, "name is null");
        this.method = checkNotNull(method, "method is null");
        this.fieldKind = checkNotNull(fieldKind, "fieldKind is null");
        this.type = TypeToken.of(checkNotNull(fieldType, "structType is null")).getRawType();

        switch (fieldKind) {
            case THRIFT_FIELD:
                // Nothing to check
                break;
            case THRIFT_UNION_ID:
                checkArgument (fieldId == Short.MIN_VALUE, "fieldId must be Short.MIN_VALUE for thrift_union_id");
                break;
        }

        this.id = fieldId;
    }

    @Override
    public FieldKind getFieldKind()
    {
        return fieldKind;
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

    public Method getMethod()
    {
        return method;
    }

    public Class<?> getType()
    {
        return type;
    }

    public boolean isGeneric()
    {
        return getMethod().getReturnType() != getMethod().getGenericReturnType();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftMethodExtractor");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", fieldKind=").append(fieldKind);
        sb.append(", method=").append(method);
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
