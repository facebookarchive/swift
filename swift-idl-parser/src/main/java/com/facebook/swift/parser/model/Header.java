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
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Header
{
    private final List<String> includes;
    private final Map<String, String> namespaces;
    private final String defaultNamespace;
    private final List<String> cppIncludes;

    public Header(List<String> includes, List<String> cppIncludes, String defaultNamespace, Map<String, String> namespaces)
    {
        this.includes = ImmutableList.copyOf(checkNotNull(includes, "includes"));
        this.cppIncludes = ImmutableList.copyOf(checkNotNull(cppIncludes, "cppIncludes"));
        this.defaultNamespace = defaultNamespace;
        this.namespaces = ImmutableMap.copyOf(checkNotNull(namespaces, "namespaces"));
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public String getNamespace(final String nameSpaceName)
    {
        String namespace = namespaces.get(nameSpaceName);
        if (namespace == null) {
            namespace = defaultNamespace;
        }
        return namespace;
    }

    public Map<String, String> getNamespaces()
    {
        return namespaces;
    }

    public List<String> getCppIncludes()
    {
        return cppIncludes;
    }

    public String getDefaultNamespace()
    {
        return defaultNamespace;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("includes", includes)
                          .add("cppIncludes", cppIncludes)
                          .add("defaultNamespace", defaultNamespace)
                          .add("namespaces", namespaces)
                          .toString();
    }
}
