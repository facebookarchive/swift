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
        return findType(thriftNamespace + "." + name);
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
