/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.metadata;

import com.google.common.base.Preconditions;

import java.lang.reflect.Method;

public class ThriftMethodExtractor implements ThriftExtraction
{
    private final short id;
    private final String name;
    private final Method method;

    public ThriftMethodExtractor(short id, String name, Method method)
    {
        Preconditions.checkArgument(id >= 0, "fieldId is negative");
        Preconditions.checkNotNull(name, "name is null");
        Preconditions.checkNotNull(method, "method is null");

        this.id = id;
        this.name = name;
        this.method = method;
    }

    @Override
    public short getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public Method getMethod()
    {
        return method;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftMethodExtractor");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", method=").append(method);
        sb.append('}');
        return sb.toString();
    }
}
