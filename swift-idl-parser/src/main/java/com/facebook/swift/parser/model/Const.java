package com.facebook.swift.parser.model;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class Const
    extends Definition
{
    private final String name;
    private final ThriftType type;
    private final ConstValue value;

    public Const(String name, ThriftType type, ConstValue value)
    {
        this.name = checkNotNull(name, "name");
        this.type = checkNotNull(type, "type");
        this.value = checkNotNull(value, "value");
    }

    public String getName()
    {
        return name;
    }

    public ThriftType getType()
    {
        return type;
    }

    public ConstValue getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("value", value)
                .toString();
    }
}
