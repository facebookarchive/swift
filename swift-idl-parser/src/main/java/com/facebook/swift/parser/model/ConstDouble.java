package com.facebook.swift.parser.model;

import com.google.common.base.Objects;

public class ConstDouble
        extends ConstValue
{
    private final double value;

    public ConstDouble(double value)
    {
        this.value = value;
    }

    @Override
    public Double value()
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
