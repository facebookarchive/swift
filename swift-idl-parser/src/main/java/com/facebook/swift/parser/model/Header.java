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
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Header
{
    private final List<String> includes;
    private final List<Namespace> namespaces;
    private final List<String> cppIncludes;

    public Header(List<String> includes, List<Namespace> namespaces, List<String> cppIncludes)
    {
        this.includes = ImmutableList.copyOf(checkNotNull(includes, "includes"));
        this.namespaces = ImmutableList.copyOf(checkNotNull(namespaces, "namespaces"));
        this.cppIncludes = ImmutableList.copyOf(checkNotNull(cppIncludes, "cppIncludes"));
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public List<Namespace> getNamespaces()
    {
        return namespaces;
    }

    public List<String> getCppIncludes()
    {
        return cppIncludes;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("includes", includes)
                .add("namespaces", namespaces)
                .add("cppIncludes", cppIncludes)
                .toString();
    }
}
