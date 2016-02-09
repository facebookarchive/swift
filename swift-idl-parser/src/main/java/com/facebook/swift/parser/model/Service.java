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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Service
        extends Definition
{
    private final String name;
    private final Optional<String> parent;
    private final List<ThriftMethod> methods;

    public Service(String name, String parent, List<ThriftMethod> methods, List<TypeAnnotation> annotations)
    {
        this.name = checkNotNull(name, "name");
        this.parent = Optional.fromNullable(parent);
        this.methods = ImmutableList.copyOf(checkNotNull(methods, "methods"));
    }

    @Override
    public String getName()
    {
        return name;
    }

    public Optional<String> getParent()
    {
        return parent;
    }

    public List<ThriftMethod> getMethods()
    {
        return methods;
    }

    @Override
    public void visit(final DocumentVisitor visitor) throws IOException
    {
        super.visit(visitor);
        Visitable.Utils.visitAll(visitor, getMethods());
    }


    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("name", name)
                          .add("parent", parent)
                          .add("methods", methods)
                          .toString();
    }
}
