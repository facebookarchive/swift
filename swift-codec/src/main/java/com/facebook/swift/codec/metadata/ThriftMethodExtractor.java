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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class ThriftMethodExtractor implements ThriftExtraction
{
    private final short id;
    private final String name;
    private final Method method;
    private final FieldType fieldType;

    public ThriftMethodExtractor(short id, String name, Method method, FieldType fieldType)
    {
        this.name = checkNotNull(name, "name is null");
        this.method = checkNotNull(method, "method is null");
        this.fieldType = checkNotNull(fieldType, "fieldType is null");

        switch (fieldType) {
            case THRIFT_FIELD:
                checkArgument(id >= 0, "fieldId is negative");
                break;
            case THRIFT_UNION_ID:
                checkArgument (id == Short.MIN_VALUE, "fieldId must be Short.MIN_VALUE for thrift_union_id");
                break;
        }

        this.id = id;
    }

    @Override
    public FieldType getType()
    {
        return fieldType;
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

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftMethodExtractor");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", fieldType=").append(fieldType);
        sb.append(", method=").append(method);
        sb.append('}');
        return sb.toString();
    }
}
