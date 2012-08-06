package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class IntegerEnumField
{
    private final String name;
    private final Optional<Long> value;

    public IntegerEnumField(String name, Long value)
    {
        this.name = checkNotNull(name, "name");
        this.value = Optional.fromNullable(value);
    }

    public String getName()
    {
        return name;
    }

    public Optional<Long> getValue()
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
