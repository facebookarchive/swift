package com.facebook.swift.service.explicitidentifiers;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import org.apache.thrift.TException;

import java.io.Closeable;

@ThriftService
public interface TestServiceClient extends Closeable
{
    public void close();

    @ThriftMethod
    void explicitParameterOrdering(
            @ThriftField(value = 30) String stringParam,
            @ThriftField(value = 10) int integerParam,
            @ThriftField(value = 20) boolean booleanParam,
            @ThriftField(value = 40) byte dummy
    ) throws TException;

    @ThriftMethod
    public void missingIncomingParameter(
            @ThriftField(value = 1) int firstParameter
    ) throws TException;

    @ThriftMethod
    public void extraIncomingParameter(
            @ThriftField(value = 1) int firstParameter,
            @ThriftField(value = 2) String secondParameter
    ) throws TException;

    @ThriftMethod
    public void missingAndReorderedParameters(
            @ThriftField(value = 1) int integerOne,
            @ThriftField(value = 2) String stringTwo
    );

    @ThriftMethod
    public void extraAndReorderedParameters(
            @ThriftField(value = 1) int integerOne,
            @ThriftField(value = 2) String stringTwo,
            @ThriftField(value = 3) boolean booleanTrue
    );

    @ThriftMethod
    public void missingInteger();

    @ThriftMethod
    public void missingStruct();

    @ThriftMethod
    public void extraStruct(CustomArgument customArgument);
}
