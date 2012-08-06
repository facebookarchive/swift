package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import static com.google.common.base.Preconditions.checkNotNull;

public class TypeAnnotation
{
    private final String name;
    private final String value;

    public TypeAnnotation(String name, String value)
    {
        this.name = checkNotNull(name, "name");
        this.value = Preconditions.checkNotNull(value, "value");
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("value", value)
                .toString();
    }
}
