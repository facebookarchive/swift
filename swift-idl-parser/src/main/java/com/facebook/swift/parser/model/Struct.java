package com.facebook.swift.parser.model;

import java.util.List;

public class Struct
        extends AbstractStruct
{
    public Struct(String name, List<ThriftField> fields, List<TypeAnnotation> annotations)
    {
        super(name, fields, annotations);
    }
}
