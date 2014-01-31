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

import com.facebook.swift.parser.model.ThriftType;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.Iterator;
import java.util.Map;

import static com.facebook.swift.generator.util.SwiftInternalStringUtils.isBlank;

/**
 * Collects typedefs for replacement at templating time.
 */
public class TypedefRegistry implements Iterable<Map.Entry<String, ThriftType>>
{
    private final Map<String, ThriftType> registry = Maps.newHashMap();

    public void addAll(final TypedefRegistry otherRegistry)
    {
        for (final Map.Entry<String, ThriftType> entry : otherRegistry) {
            add(entry.getKey(), entry.getValue());
        }
    }

    public void add(final SwiftJavaType type, final ThriftType thriftType)
    {
        Preconditions.checkState(!registry.containsKey(type.getKey()), "The type %s was already registered!", type);
        add(type.getKey(), thriftType);
    }

    private void add(final String key, final ThriftType thriftType)
    {
        Preconditions.checkArgument(!isBlank(key), "key can not be empty!");
        registry.put(key, thriftType);
    }

    public ThriftType findType(final String defaultNamespace, final String typeName)
    {
        String key = typeName;
        if (!key.contains(".")) {
            key = defaultNamespace + "." + typeName;
        }
        return findType(key);
    }

    public ThriftType findType(final String key)
    {
        return registry.get(key);
    }

    @Override
    public Iterator<Map.Entry<String, ThriftType>> iterator()
    {
        return registry.entrySet().iterator();
    }
}
