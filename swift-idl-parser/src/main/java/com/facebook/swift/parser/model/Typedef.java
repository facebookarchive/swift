package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import static com.google.common.base.Preconditions.checkNotNull;

public class Typedef
        extends Definition
{
    private final String name;
    private final ThriftType type;

    public Typedef(String name, ThriftType type)
    {
        this.name = checkNotNull(name, "name");
        this.type = Preconditions.checkNotNull(type, "type");
    }

    public String getName()
    {
        return name;
    }

    public ThriftType getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .toString();
    }
}
