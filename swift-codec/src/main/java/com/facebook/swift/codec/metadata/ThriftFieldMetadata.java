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

import com.google.common.collect.ImmutableList;

import javax.annotation.concurrent.Immutable;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ThriftFieldMetadata defines a single thrift field including the value extraction and injection
 * points.
 */
@Immutable
public class ThriftFieldMetadata
{
    private final short id;
    private final ThriftType type;
    private final String name;
    private final List<ThriftInjection> injections;
    private final ThriftExtraction extraction;
    private final TypeCoercion coercion;

    public ThriftFieldMetadata(
            short id,
            ThriftType type,
            String name,
            List<ThriftInjection> injections,
            ThriftExtraction extraction,
            TypeCoercion coercion
    )
    {
        checkArgument(id >= 0, "id is negative");
        checkNotNull(type, "type is null");
        checkNotNull(name, "name is null");
        checkNotNull(injections, "injections is null");
        checkArgument(!injections.isEmpty() || extraction != null, "A thrift field must have an injection or extraction point");

        this.id = id;
        this.type = type;
        this.name = name;
        this.injections = ImmutableList.copyOf(injections);
        this.extraction = extraction;
        this.coercion = coercion;
    }

    public short getId()
    {
        return id;
    }

    public ThriftType getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public boolean isReadable()
    {
        return extraction != null;
    }

    public boolean isWritable()
    {
        return !injections.isEmpty();
    }

    public boolean isReadOnly()
    {
        return injections.isEmpty();
    }

    public boolean isWriteOnly()
    {
        return extraction == null;
    }

    public List<ThriftInjection> getInjections()
    {
        return injections;
    }

    public ThriftExtraction getExtraction()
    {
        return extraction;
    }

    public TypeCoercion getCoercion()
    {
        return coercion;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftFieldMetadata");
        sb.append("{id=").append(id);
        sb.append(", type=").append(type);
        sb.append(", name='").append(name).append('\'');
        sb.append(", injections=").append(injections);
        sb.append(", extraction=").append(extraction);
        sb.append(", coercion=").append(coercion);
        sb.append('}');
        return sb.toString();
    }
}
