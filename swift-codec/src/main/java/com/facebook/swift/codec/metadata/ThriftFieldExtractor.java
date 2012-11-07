/*
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
package com.facebook.swift.codec.metadata;

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Field;

@Immutable
public class ThriftFieldExtractor implements ThriftExtraction
{
    private final short id;
    private final String name;
    private final Field field;

    public ThriftFieldExtractor(short id, String name, Field field)
    {
        Preconditions.checkArgument(id >= 0, "fieldId is negative");
        Preconditions.checkNotNull(name, "name is null");
        Preconditions.checkNotNull(field, "field is null");

        this.id = id;
        this.name = name;
        this.field = field;
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

    public Field getField()
    {
        return field;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftFieldExtractor");
        sb.append("{id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", field=").append(field.getDeclaringClass().getSimpleName()).append(".").append(field.getName());
        sb.append('}');
        return sb.toString();
    }
}
