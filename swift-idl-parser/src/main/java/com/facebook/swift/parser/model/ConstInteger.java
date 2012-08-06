package com.facebook.swift.parser.model;

import com.google.common.base.Objects;

public class ConstInteger
        extends ConstValue
{
    private final long value;

    public ConstInteger(long value)
    {
        this.value = value;
    }

    @Override
    public Long value()
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
