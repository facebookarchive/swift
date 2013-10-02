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
package com.facebook.swift.generator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.Iterator;
import java.util.Map;

/**
 * Collects all the various custom types found in the IDL definition files.
 */
public class TypeRegistry implements Iterable<SwiftJavaType>
{
    private final Map<String, SwiftJavaType> registry = Maps.newHashMap();

    public TypeRegistry()
    {
    }

    public void addAll(final TypeRegistry otherRegistry)
    {
        for (SwiftJavaType type : otherRegistry) {
            add(type);
        }
    }

    public void add(final SwiftJavaType type)
    {
        Preconditions.checkState(!registry.containsKey(type.getKey()), "The type %s was already registered!", type);
        registry.put(type.getKey(), type);
    }

    public SwiftJavaType findType(final String thriftNamespace, final String name)
    {
        if (name == null) {
            return null;
        }

        if (name.contains(".")) {
            // If the name contains a '.' it already has a namespace prepended
            return findType(name);
        }
        else {
            // Otherwise, use the default namespace
            return findType(thriftNamespace + "." + name);
        }
    }

    public SwiftJavaType findType(final String key)
    {
        if (key == null) {
            return null;
        }

        Preconditions.checkState(registry.containsKey(key), "key %s is not known!", key);
        return registry.get(key);
    }

    @Override
    public Iterator<SwiftJavaType> iterator()
    {
        return registry.values().iterator();
    }
}
