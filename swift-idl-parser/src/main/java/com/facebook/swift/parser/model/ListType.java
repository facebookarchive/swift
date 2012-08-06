package com.facebook.swift.parser.model;

import com.google.common.base.Objects;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ListType
        extends ContainerType
{
    private final ThriftType type;

    public ListType(ThriftType type, String cppType, List<TypeAnnotation> annotations)
    {
        super(cppType, annotations);
        this.type = checkNotNull(type, "type");
    }

    public ThriftType getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("type", type)
                .add("cppType", cppType)
                .add("annotations", annotations)
                .toString();
    }
}
