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

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class IntegerEnumField
{
    private final String name;
    private final Optional<Long> value;

    private final long implicitValue;

    public IntegerEnumField(String name, Long explicitValue, Long defaultValue)
    {
        this.name = checkNotNull(name, "name");
        this.value = Optional.fromNullable(explicitValue);
        if (this.value.isPresent()) {
            this.implicitValue = this.value.get();
        } else {
            this.implicitValue = defaultValue;
        }
    }

    public String getName()
    {
        return name;
    }

    public Optional<Long> getExplicitValue()
    {
        return value;
    }

    public long getValue()
    {
        return implicitValue;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("value", value)
                .toString();
    }
}
