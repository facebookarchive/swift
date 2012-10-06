package com.facebook.swift.service.explicitidentifiers;

import com.facebook.swift.service.base.TestSuiteBase;
import org.apache.thrift.TException;
import org.testng.annotations.Test;

public class TestSuite extends TestSuiteBase<TestServiceHandler, TestServiceClient>
{
    public TestSuite()
    {
        super(TestServiceHandler.class, TestServiceClient.class);
    }

    // Client passes all parameters, but in a different order than the server expects
    @Test
    public void testExplicitParameterOrdering()
            throws TException
    {
        getClient().explicitParameterOrdering("STRING", Integer.MAX_VALUE, Boolean.TRUE, Byte.MAX_VALUE);
    }

    // Client passes only one parameter, server expects two
    @Test
    public void testMissingParameter()
        throws TException
    {
        getClient().missingIncomingParameter(1);
    }

    // Client passes two parameters, server expects only one
    @Test
    public void testExtraParameter()
        throws TException
    {
        getClient().extraIncomingParameter(1, "2");
    }

    // Client passes two parameters in ID order (1,2), server expects three in ID order (3,2,1)
    @Test
    public void testMissingAndReorderedParameters()
    {
        getClient().missingAndReorderedParameters(1, "2");
    }

    // Client passes three parameters in ID order (1,2,3), server expects two in ID order (3,2)
    @Test
    public void testExtraAndReorderedParameters()
    {
        getClient().extraAndReorderedParameters(1, "2", true);
    }
}
