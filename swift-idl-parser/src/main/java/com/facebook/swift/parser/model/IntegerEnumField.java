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
package com.facebook.swift.parser.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class IntegerEnumField
{
    private final String name;
    private final Optional<Long> explicitValue;

    private final long effectiveValue;

    public IntegerEnumField(String name, Long explicitValue, Long defaultValue)
    {
        this.name = checkNotNull(name, "name");
        this.explicitValue = Optional.fromNullable(explicitValue);
        this.effectiveValue = this.explicitValue.isPresent() ? this.explicitValue.get() : defaultValue;
    }

    public String getName()
    {
        return name;
    }

    public Optional<Long> getExplicitValue()
    {
        return explicitValue;
    }

    public long getValue()
    {
        return effectiveValue;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("name", name)
                          .add("value", effectiveValue)
                          .add("explicitValue", explicitValue)
                          .toString();
    }
}
