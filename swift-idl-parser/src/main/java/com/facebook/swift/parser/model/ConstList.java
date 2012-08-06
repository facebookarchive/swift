package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ConstList
        extends ConstValue
{
    private final List<ConstValue> value;

    public ConstList(List<ConstValue> value)
    {
        this.value = ImmutableList.copyOf(checkNotNull(value, "value"));
    }

    @Override
    public List<ConstValue> value()
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
