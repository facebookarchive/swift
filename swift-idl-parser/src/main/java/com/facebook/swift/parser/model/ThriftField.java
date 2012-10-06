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
import com.google.common.base.Optional;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThriftField
{


    public static enum Required
    {
        REQUIRED, OPTIONAL, NONE;
    }
    private final String name;

    private final ThriftType type;
    private final Optional<Long> explicitIdentifiier;

    private final long identifier;

    private final Required required;
    private final Optional<ConstValue> value;
    private final List<TypeAnnotation> annotations;
    public ThriftField(
            String name,
            ThriftType type,
            Long explicitIdentifiier,
            long defaultIdentifier,
            Required required,
            ConstValue value,
            List<TypeAnnotation> annotations)
    {
        this.name = checkNotNull(name, "name");
        this.type = checkNotNull(type, "type");
        this.explicitIdentifiier = Optional.fromNullable(explicitIdentifiier);
        this.identifier = (explicitIdentifiier == null) ? defaultIdentifier : explicitIdentifiier;
        this.required = checkNotNull(required, "required");
        this.value = Optional.fromNullable(value);
        this.annotations = checkNotNull(annotations, "annotations");
    }

    public String getName()
    {
        return name;
    }

    public ThriftType getType()
    {
        return type;
    }

    public long getIdentifier()
    {
        return identifier;
    }

    public Optional<Long> getExplicitIdentifiier()
    {
        return explicitIdentifiier;
    }

    public Required getRequired()
    {
        return required;
    }

    public Optional<ConstValue> getValue()
    {
        return value;
    }

    public List<TypeAnnotation> getAnnotations()
    {
        return annotations;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("explicitIdentifiier", explicitIdentifiier)
                .add("required", required)
                .add("value", value)
                .add("annotations", annotations)
                .toString();
    }
}
