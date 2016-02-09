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
import com.google.common.base.Preconditions;

import static com.google.common.base.Preconditions.checkNotNull;

public class Typedef
        extends Definition
{
    private final String name;
    private final ThriftType type;

    public Typedef(String name, ThriftType type)
    {
        this.name = checkNotNull(name, "name");
        this.type = Preconditions.checkNotNull(type, "type");
    }

    @Override
    public String getName()
    {
        return name;
    }

    public ThriftType getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("name", name)
                          .add("type", type)
                          .toString();
    }
}
