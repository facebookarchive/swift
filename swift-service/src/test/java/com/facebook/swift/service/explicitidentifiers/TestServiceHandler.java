package com.facebook.swift.service.explicitidentifiers;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;

import static org.testng.Assert.*;

@ThriftService
public class TestServiceHandler
{
    @ThriftMethod
    public void explicitParameterOrdering(
            @ThriftField(value = 20) boolean booleanParam,
            @ThriftField(value = 30) String stringParam,
            @ThriftField(value = 10) int integerParam,
            @ThriftField(value = 40) byte dummy
    ) {}

    @ThriftMethod
    public void missingIncomingParameter(
            @ThriftField(value = 1) int integerOne,
            @ThriftField(value = 2) String defaultString
    ) {
        assertEquals(integerOne, 1);
        assertEquals(defaultString, null);
    }

    @ThriftMethod
    public void extraIncomingParameter(
            @ThriftField(value = 1) int integerOne
    ) {
        assertEquals(integerOne, 1);
    }

    @ThriftMethod
    public void missingAndReorderedParameters(
            @ThriftField(value = 3) boolean defaultBoolean,
            @ThriftField(value = 2) String stringTwo,
            @ThriftField(value = 1) int integerOne
    ) {
        assertEquals(integerOne, 1);
        assertEquals(stringTwo, "2");
        assertEquals(defaultBoolean, false);
    }

    @ThriftMethod
    public void extraAndReorderedParameters(
            @ThriftField(value = 3) boolean booleanTrue,
            @ThriftField(value = 2) String stringTwo
    ) {
        assertEquals(stringTwo, "2");
        assertEquals(booleanTrue, true);
    }
}
