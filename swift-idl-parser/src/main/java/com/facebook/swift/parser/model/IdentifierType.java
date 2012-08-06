package com.facebook.swift.parser.model;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class IdentifierType
        extends ThriftType
{
    private final String name;

    public IdentifierType(String name)
    {
        this.name = checkNotNull(name, "name");
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .toString();
    }
}
