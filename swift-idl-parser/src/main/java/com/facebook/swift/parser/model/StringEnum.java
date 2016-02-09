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
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class StringEnum
        extends Definition
{
    private final String name;
    private final List<String> values;

    public StringEnum(String name, List<String> values)
    {
        this.name = checkNotNull(name, "name");
        this.values = ImmutableList.copyOf(checkNotNull(values, "values"));
    }

    @Override
    public String getName()
    {
        return name;
    }

    public List<String> getValues()
    {
        return values;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("name", name)
                          .add("values", values)
                          .toString();
    }
}
