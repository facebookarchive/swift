package com.facebook.swift.parser.model;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class Namespace
{
    private final String type;
    private final String value;

    public Namespace(String type, String value)
    {
        this.type = checkNotNull(type, "type");
        this.value = checkNotNull(value, "value");
    }

    public String getType()
    {
        return type;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("type", type)
                .add("value", value)
                .toString();
    }
}
