package com.facebook.swift.parser.model;

import com.google.common.base.Objects;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MapType
        extends ContainerType
{
    private final ThriftType keyType;
    private final ThriftType valueType;

    public MapType(ThriftType keyType, ThriftType valueType, String cppType, List<TypeAnnotation> annotations)
    {
        super(cppType, annotations);
        this.keyType = checkNotNull(keyType, "keyType");
        this.valueType = checkNotNull(valueType, "valueType");
    }

    public ThriftType getKeyType()
    {
        return keyType;
    }

    public ThriftType getValueType()
    {
        return valueType;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("keyType", keyType)
                .add("valueType", valueType)
                .add("cppType", cppType)
                .add("annotations", annotations)
                .toString();
    }
}
