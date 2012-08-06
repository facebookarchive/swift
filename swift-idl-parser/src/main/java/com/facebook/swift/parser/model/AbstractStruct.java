package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractStruct
        extends Definition
{
    private final String name;
    private final List<ThriftField> fields;
    private final List<TypeAnnotation> annotations;

    public AbstractStruct(String name, List<ThriftField> fields, List<TypeAnnotation> annotations)
    {
        this.name = checkNotNull(name, "name");
        this.fields = ImmutableList.copyOf(checkNotNull(fields, "fields"));
        this.annotations = ImmutableList.copyOf(checkNotNull(annotations, "annotations"));
    }

    public String getName()
    {
        return name;
    }

    public List<ThriftField> getFields()
    {
        return fields;
    }

    public List<TypeAnnotation> getAnnotations()
    {
        return annotations;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("fields", fields)
                .add("annotations", annotations)
                .toString();
    }
}
