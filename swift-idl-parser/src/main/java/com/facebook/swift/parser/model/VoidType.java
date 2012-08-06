package com.facebook.swift.parser.model;

import com.google.common.base.Objects;

public class VoidType
    extends ThriftType
{
    @Override
    public String toString()
    {
        return Objects.toStringHelper(this).toString();
    }
}
