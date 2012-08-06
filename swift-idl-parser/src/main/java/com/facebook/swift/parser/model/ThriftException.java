package com.facebook.swift.parser.model;

import java.util.List;

public class ThriftException
        extends AbstractStruct
{
    public ThriftException(String name, List<ThriftField> fields, List<TypeAnnotation> annotations)
    {
        super(name, fields, annotations);
    }
}
