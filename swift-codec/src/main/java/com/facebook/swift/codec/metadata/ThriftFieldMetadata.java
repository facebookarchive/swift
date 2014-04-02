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
import com.google.common.collect.ImmutableList;

import javax.annotation.concurrent.Immutable;

import java.util.List;
import java.util.Objects;

import static com.facebook.swift.codec.ThriftField.Requiredness;
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
    private final ThriftType thriftType;
    private final String name;
    private final FieldKind fieldKind;
    private final List<ThriftInjection> injections;
    private final Optional<ThriftConstructorInjection> constructorInjection;
    private final Optional<ThriftMethodInjection> methodInjection;
    private final Optional<ThriftExtraction> extraction;
    private final Optional<TypeCoercion> coercion;
    private final ImmutableList<String> documentation;
    private final Requiredness requiredness;

    public ThriftFieldMetadata(
            short id,
            Requiredness requiredness,
            ThriftType thriftType,
            String name,
            FieldKind fieldKind,
            List<ThriftInjection> injections,
            Optional<ThriftConstructorInjection> constructorInjection,
            Optional<ThriftMethodInjection> methodInjection,
            Optional<ThriftExtraction> extraction,
            Optional<TypeCoercion> coercion
    )
    {
        this.requiredness = requiredness;
        this.thriftType= checkNotNull(thriftType, "thriftType is null");
        this.fieldKind = checkNotNull(fieldKind, "type is null");
        this.name = checkNotNull(name, "name is null");
        this.injections = ImmutableList.copyOf(checkNotNull(injections, "injections is null"));
        this.constructorInjection = checkNotNull(constructorInjection, "constructorInjection is null");
        this.methodInjection = checkNotNull(methodInjection, "methodInjection is null");

        this.extraction = checkNotNull(extraction, "extraction is null");
        this.coercion = checkNotNull(coercion, "coercion is null");

        switch (fieldKind) {
            case THRIFT_FIELD:
                checkArgument(id >= 0, "id is negative");
                break;
            case THRIFT_UNION_ID:
                checkArgument(id == Short.MIN_VALUE, "thrift union id must be Short.MIN_VALUE");
                break;
        }

        checkArgument(!injections.isEmpty()
                      || extraction.isPresent()
                      || constructorInjection.isPresent()
                      || methodInjection.isPresent(), "A thrift field must have an injection or extraction point");

        this.id = id;

        if (extraction.isPresent()) {
            if (extraction.get() instanceof ThriftFieldExtractor) {
                ThriftFieldExtractor e = (ThriftFieldExtractor)extraction.get();
                this.documentation = ThriftCatalog.getThriftDocumentation(e.getField());
            } else if (extraction.get() instanceof ThriftMethodExtractor) {
                ThriftMethodExtractor e = (ThriftMethodExtractor)extraction.get();
                this.documentation = ThriftCatalog.getThriftDocumentation(e.getMethod());
            }
            else {
                this.documentation = ImmutableList.of();
            }
        } else {
            // no extraction = no documentation
            this.documentation = ImmutableList.of();
        }
    }

    public short getId()
    {
        return id;
    }

    public ThriftType getThriftType()
    {
        return thriftType;
    }

    public Requiredness getRequiredness() { return requiredness; }

    public String getName()
    {
        return name;
    }

    public FieldKind getType()
    {
        return fieldKind;
    }

    public boolean isInternal()
    {
        switch (getType()) {
            // These are normal thrift fields (i.e. they should be emitted by the swift2thrift generator)
            case THRIFT_FIELD:
                return false;

            // Other fields types are used internally in swift, but do not make up part of the external
            // thrift interface
            default:
                return true;
        }
    }

    public boolean isReadOnly()
    {
        return injections.isEmpty() && !constructorInjection.isPresent() && !methodInjection.isPresent();
    }

    public boolean isWriteOnly()
    {
        return !extraction.isPresent();
    }

    public List<ThriftInjection> getInjections()
    {
        return injections;
    }

    public Optional<ThriftConstructorInjection> getConstructorInjection()
    {
        return constructorInjection;
    }

    public Optional<ThriftMethodInjection> getMethodInjection()
    {
        return methodInjection;
    }

    public Optional<ThriftExtraction> getExtraction()
    {
        return extraction;
    }

    public Optional<TypeCoercion> getCoercion()
    {
        return coercion;
    }

    public ImmutableList<String> getDocumentation()
    {
        return documentation;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftFieldMetadata");
        sb.append("{id=").append(id);
        sb.append(", thriftType=").append(thriftType);
        sb.append(", name='").append(name).append('\'');
        sb.append(", fieldKind=").append(fieldKind);
        sb.append(", injections=").append(injections);
        sb.append(", constructorInjection=").append(constructorInjection);
        sb.append(", methodInjection=").append(methodInjection);
        sb.append(", extraction=").append(extraction);
        sb.append(", coercion=").append(coercion);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, thriftType, name);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ThriftFieldMetadata other = (ThriftFieldMetadata) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.thriftType, other.thriftType) && Objects.equals(this.name, other.name);
    }

    public static Function<ThriftFieldMetadata, Short> getIdGetter()
    {
        return new Function<ThriftFieldMetadata, Short>() {
            @Override
            public Short apply(ThriftFieldMetadata metadata)
            {
                return metadata.getId();
            }
        };
    }

    public static Predicate<ThriftFieldMetadata> isTypePredicate(final FieldKind type)
    {
        return new Predicate<ThriftFieldMetadata>() {
            @Override
            public boolean apply(ThriftFieldMetadata fieldMetadata)
            {
                return fieldMetadata.getType() == type;
            }
        };
    }
}
