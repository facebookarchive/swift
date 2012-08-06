package com.facebook.swift.parser.model;

import java.util.List;

public class Union
        extends AbstractStruct
{
    public Union(String name, List<ThriftField> fields, List<TypeAnnotation> annotations)
    {
        super(name, fields, annotations);
    }
}
