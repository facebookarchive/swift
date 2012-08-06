package com.facebook.swift.parser.model;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class ConstString
        extends ConstValue
{
    private final String value;

    public ConstString(String value)
    {
        this.value = checkNotNull(value, "value");
    }

    @Override
    public String value()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("value", value)
                .toString();
    }
}
