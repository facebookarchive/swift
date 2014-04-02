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
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.inject.internal.asm.$AnnotationVisitor;

import javax.annotation.Nullable;

import java.lang.reflect.Type;

import static com.facebook.swift.codec.ThriftField.Requiredness;
import static com.google.common.base.Preconditions.checkNotNull;

abstract class FieldMetadata
{
    private Short id;
    private String name;
    private Requiredness requiredness;
    private final FieldKind type;

    protected FieldMetadata(ThriftField annotation, FieldKind type)
    {
        this.type = type;

        switch (type) {
            case THRIFT_FIELD:
                if (annotation != null) {
                    if (annotation.value() != Short.MIN_VALUE) {
                        id = annotation.value();
                    }
                    if (!annotation.name().isEmpty()) {
                        name = annotation.name();
                    }
                    requiredness = checkNotNull(annotation.requiredness());
                }
                break;
            case THRIFT_UNION_ID:
                id = Short.MIN_VALUE;
                name = "_union_id";
                break;
            default:
                throw new IllegalArgumentException("Encountered field metadata type " + type);
        }
    }

    public Short getId()
    {
        return id;
    }

    public void setId(short id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public FieldKind getType()
    {
        return type;
    }

    public abstract Type getJavaType();

    public abstract String extractName();

    static <T extends FieldMetadata> Function<T, Optional<Short>> getThriftFieldId()
    {
        return new Function<T, Optional<Short>>()
        {
            @Override
            public Optional<Short> apply(@Nullable T input)
            {
                if (input == null) {
                    return Optional.absent();
                }
                Short value = input.getId();
                return Optional.fromNullable(value);
            }
        };
    }

    static <T extends FieldMetadata> Function<T, String> getThriftFieldName()
    {
        return new Function<T, String>()
        {
            @Override
            public String apply(@Nullable T input)
            {
                if (input == null) {
                    return null;
                }
                return input.getName();
            }
        };
    }

    static <T extends FieldMetadata> Function<T, String> getOrExtractThriftFieldName()
    {
        return new Function<T, String>()
        {
            @Override
            public String apply(@Nullable T input)
            {
                if (input == null) {
                    return null;
                }
                String name = input.getName();
                if (name == null) {
                    name = input.extractName();
                }
                if (name == null) {
                    throw new NullPointerException(String.valueOf("name is null"));
                }
                return name;
            }
        };
    }

    static <T extends FieldMetadata> Function<T, String> extractThriftFieldName()
    {
        return new Function<T, String>()
        {
            @Override
            public String apply(@Nullable T input)
            {
                if (input == null) {
                    return null;
                }
                return input.extractName();
            }
        };
    }

    static <T extends FieldMetadata> Function<T, Requiredness> getThriftFieldRequiredness()
    {
        return new Function<T, Requiredness>()
        {
            @Nullable
            @Override
            public Requiredness apply(@Nullable T input)
            {
                return input.getRequiredness();
            }
        };
    }

    public static Predicate<FieldMetadata> isType(final FieldKind type)
    {
        return new Predicate<FieldMetadata>() {
            @Override
            public boolean apply(FieldMetadata fieldMetadata)
            {
                return fieldMetadata.getType() == type;
            }
        };
    }

    public Requiredness getRequiredness()
    {
        return requiredness;
    }

    public void setRequiredness(Requiredness requiredness)
    {
        this.requiredness = requiredness;
    }
}
