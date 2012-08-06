package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Service
        extends Definition
{
    private final String name;
    private final Optional<String> parent;
    private final List<ThriftMethod> methods;

    public Service(String name, String parent, List<ThriftMethod> methods)
    {
        this.name = checkNotNull(name, "name");
        this.parent = Optional.fromNullable(parent);
        this.methods = ImmutableList.copyOf(checkNotNull(methods, "methods"));
    }

    public String getName()
    {
        return name;
    }

    public Optional<String> getParent()
    {
        return parent;
    }

    public List<ThriftMethod> getMethods()
    {
        return methods;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("parent", parent)
                .add("methods", methods)
                .toString();
    }
}
