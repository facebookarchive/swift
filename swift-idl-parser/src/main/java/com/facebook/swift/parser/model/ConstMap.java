package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ConstMap
        extends ConstValue
{
    private final Map<ConstValue, ConstValue> value;

    public ConstMap(Map<ConstValue, ConstValue> value)
    {
        this.value = ImmutableMap.copyOf(checkNotNull(value, "value"));
    }

    @Override
    public Map<ConstValue, ConstValue> value()
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
