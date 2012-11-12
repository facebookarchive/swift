package com.facebook.swift.service.explicitidentifiers;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

@ThriftStruct
public class CustomArgument
{
    public CustomArgument() {
        this.integerField = 0;
        this.stringField = null;
    }

    public CustomArgument(int integerField, String stringField) {
        this.integerField = integerField;
        this.stringField = stringField;
    }

    @ThriftField(value = 1)
    public int integerField;

    @ThriftField(value = 2)
    public String stringField;
}
