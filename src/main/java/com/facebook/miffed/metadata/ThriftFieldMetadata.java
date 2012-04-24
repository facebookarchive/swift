/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.metadata;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class ThriftFieldMetadata
{
    private final short id;
    private final ThriftType type;
    private final String name;
    private final List<ThriftInjection> injections;
    private final ThriftExtraction extraction;

    public ThriftFieldMetadata(short id, ThriftType type, String name, List<ThriftInjection> injections, ThriftExtraction extraction)
    {
        Preconditions.checkArgument(id >= 0, "id is negative");
        Preconditions.checkNotNull(id, "id is null");
        Preconditions.checkNotNull(name, "name is null");
        Preconditions.checkNotNull(injections, "injections is null");
        Preconditions.checkArgument(!injections.isEmpty() || extraction != null, "A thrift field must have an injection or extraction point");

        this.id = id;
        this.type = type;
        this.name = name;
        this.injections = ImmutableList.copyOf(injections);
        this.extraction = extraction;
    }

    public short getId()
    {
        return id;
    }

    public ThriftType getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public boolean isReadable()
    {
        return extraction != null;
    }

    public boolean isWritable()
    {
        return !injections.isEmpty();
    }

    public boolean isReadOnly()
    {
        return injections.isEmpty();
    }

    public boolean isWriteOnly()
    {
        return extraction == null;
    }

    public List<ThriftInjection> getInjections()
    {
        return injections;
    }

    public ThriftExtraction getExtraction()
    {
        return extraction;
    }
}
