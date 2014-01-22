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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import javax.annotation.concurrent.Immutable;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import static com.facebook.swift.codec.metadata.ThriftFieldMetadata.isTypePredicate;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.uniqueIndex;

@Immutable
public class ThriftStructMetadata<T>
{
    public static enum MetadataType {
        STRUCT, UNION;
    }

    private final String structName;
    private final Class<T> structClass;

    private final Class<?> builderClass;
    private final MetadataType metadataType;
    private final Optional<ThriftMethodInjection> builderMethod;

    private final ImmutableList<String> documentation;
    private final SortedMap<Short, ThriftFieldMetadata> fields;

    private final Optional<ThriftConstructorInjection> constructorInjection;
    private final List<ThriftMethodInjection> methodInjections;

    public ThriftStructMetadata(
            String structName,
            Class<T> structClass,
            Class<?> builderClass,
            MetadataType metadataType,
            Optional<ThriftMethodInjection> builderMethod,
            List<String> documentation,
            List<ThriftFieldMetadata> fields,
            Optional<ThriftConstructorInjection> constructorInjection,
            List<ThriftMethodInjection> methodInjections)
    {
        this.builderClass = builderClass;
        this.builderMethod = checkNotNull(builderMethod, "builderMethod is null");
        this.structName = checkNotNull(structName, "structName is null");
        this.metadataType = checkNotNull(metadataType, "metadataType is null");
        this.structClass = checkNotNull(structClass, "structClass is null");
        this.constructorInjection = checkNotNull(constructorInjection, "constructorInjection is null");
        this.documentation = ImmutableList.copyOf(checkNotNull(documentation, "documentation is null"));
        this.fields = ImmutableSortedMap.copyOf(uniqueIndex(checkNotNull(fields, "fields is null"), new Function<ThriftFieldMetadata, Short>()
        {
            @Override
            public Short apply(ThriftFieldMetadata input)
            {
                return input.getId();
            }
        }));
        this.methodInjections = ImmutableList.copyOf(checkNotNull(methodInjections, "methodInjections is null"));
    }

    public String getStructName()
    {
        return structName;
    }

    public Class<T> getStructClass()
    {
        return structClass;
    }

    public Class<?> getBuilderClass()
    {
        return builderClass;
    }

    public MetadataType getMetadataType()
    {
        return metadataType;
    }

    public Optional<ThriftMethodInjection> getBuilderMethod()
    {
        return builderMethod;
    }

    public ThriftFieldMetadata getField(int id)
    {
        return fields.get((short) id);
    }

    public ImmutableList<String> getDocumentation()
    {
        return documentation;
    }

    public Collection<ThriftFieldMetadata> getFields(FieldType type)
    {
        return Collections2.filter(getFields(), isTypePredicate(type));
    }

    public Collection<ThriftFieldMetadata> getFields()
    {
        return fields.values();
    }

    public Optional<ThriftConstructorInjection> getConstructorInjection()
    {
        return constructorInjection;
    }

    public List<ThriftMethodInjection> getMethodInjections()
    {
        return methodInjections;
    }

    public boolean isException()
    {
        return Exception.class.isAssignableFrom(structClass);
    }

    public boolean isUnion()
    {
        return !isException() && getMetadataType() == MetadataType.UNION;
    }

    public boolean isStruct()
    {
        return !isException() && getMetadataType() == MetadataType.STRUCT;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftStructMetadata");
        sb.append("{structName='").append(structName).append('\'');
        sb.append(", structClass=").append(structClass);
        sb.append(", builderClass=").append(builderClass);
        sb.append(", builderMethod=").append(builderMethod);
        sb.append(", fields=").append(fields);
        sb.append(", constructorInjection=").append(constructorInjection);
        sb.append(", methodInjections=").append(methodInjections);
        sb.append('}');
        return sb.toString();
    }
}
