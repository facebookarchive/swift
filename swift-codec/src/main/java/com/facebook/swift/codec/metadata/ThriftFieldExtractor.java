/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.metadata;

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Field;

@Immutable
public class ThriftFieldExtractor implements ThriftExtraction
{
    private final short id;
    private final String name;
    private final Field field;

    public ThriftFieldExtractor(short id, String name, Field field)
    {
        Preconditions.checkArgument(id >= 0, "fieldId is negative");
        Preconditions.checkNotNull(name, "name is null");
        Preconditions.checkNotNull(field, "field is null");

        this.id = id;
        this.name = name;
        this.field = field;
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

    public Field getField()
    {
        return field;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftFieldExtractor");
        sb.append("{id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", field=").append(field.getDeclaringClass().getSimpleName()).append(".").append(field.getName());
        sb.append('}');
        return sb.toString();
    }
}
