package com.facebook.swift.parser.util;

import com.facebook.swift.parser.model.ThriftField;

import java.util.ArrayList;

public class ThriftFieldList extends ArrayList<ThriftField>
{
    private long nextImplicitFieldId = 1;

    @Override
    public boolean add(ThriftField thriftField)
    {
        boolean success = super.add(thriftField);
        nextImplicitFieldId = thriftField.getIdentifier() + 1;
        return success;
    }

    public long getNextImplicitFieldId() {
        return nextImplicitFieldId;
    }
}
