package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class StringEnum
        extends Definition
{
    private final String name;
    private final List<String> values;

    public StringEnum(String name, List<String> values)
    {
        this.name = checkNotNull(name, "name");
        this.values = ImmutableList.copyOf(checkNotNull(values, "values"));
    }

    public String getName()
    {
        return name;
    }

    public List<String> getValues()
    {
        return values;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("values", values)
                .toString();
    }
}
