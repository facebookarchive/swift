package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class IntegerEnum
        extends Definition
{
    private final String name;
    private final List<IntegerEnumField> fields;

    public IntegerEnum(String name, List<IntegerEnumField> fields)
    {
        this.name = checkNotNull(name, "name");
        this.fields = ImmutableList.copyOf(checkNotNull(fields, "fields"));
    }

    public String getName()
    {
        return name;
    }

    public List<IntegerEnumField> getFields()
    {
        return fields;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("fields", fields)
                .toString();
    }
}
