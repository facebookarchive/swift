package com.facebook.swift.parser.model;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ContainerType
        extends ThriftType
{
    protected final Optional<String> cppType;
    protected final List<TypeAnnotation> annotations;

    public ContainerType(String cppType, List<TypeAnnotation> annotations)
    {
        this.cppType = Optional.fromNullable(cppType);
        this.annotations = ImmutableList.copyOf(checkNotNull(annotations, "annotations"));
    }

    public Optional<String> getCppType()
    {
        return cppType;
    }

    public List<TypeAnnotation> getAnnotations()
    {
        return annotations;
    }
}
