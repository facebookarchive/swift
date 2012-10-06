package com.facebook.swift.parser.util;

import com.facebook.swift.parser.model.IntegerEnumField;

import java.util.ArrayList;

public class IntegerEnumFieldList extends ArrayList<IntegerEnumField>
{
    private long nextImplicitEnumerationValue = 1;

    @Override
    public boolean add(IntegerEnumField integerEnumField)
    {
        boolean success = super.add(integerEnumField);
        nextImplicitEnumerationValue = integerEnumField.getValue() + 1;
        return success;
    }

    public long getNextImplicitEnumerationValue()
    {
        return nextImplicitEnumerationValue;
    }
}
