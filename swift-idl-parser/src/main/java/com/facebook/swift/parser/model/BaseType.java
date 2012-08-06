package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class BaseType
        extends ThriftType
{
    public static enum Type
    {
        BOOL, BYTE, I16, I32, I64, DOUBLE, STRING, BINARY
    }

    private final Type type;
    private final List<TypeAnnotation> annotations;

    public BaseType(Type type, List<TypeAnnotation> annotations)
    {
        this.type = checkNotNull(type, "type");
        this.annotations = ImmutableList.copyOf(checkNotNull(annotations, "annotations"));
    }

    public Type getType()
    {

        return type;
    }

    public List<TypeAnnotation> getAnnotations()
    {
        return annotations;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("type", type)
                .add("annotations", annotations)
                .toString();
    }
}
