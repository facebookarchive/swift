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

import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.facebook.swift.parser.visitor.Visitable;
import com.google.common.base.MoreObjects;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class TypeAnnotation implements Visitable
{
    private final String name;
    private final String value;

    public TypeAnnotation(String name, String value)
    {
        this.name = checkNotNull(name, "name");
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("name", name)
                          .add("value", value)
                          .toString();
    }

    @Override
    public void visit(final DocumentVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }
}
