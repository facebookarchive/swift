package com.facebook.nifty.core;

import org.apache.thrift.TProcessor;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestServerDefBuilder
{
    @Test
    public void testServerDefBuilderWithoutProcessor()
    {
        try {
            new ThriftServerDefBuilder().build();
        }
        catch (Exception e) {
            return;
        }
        Assert.fail();
    }

    @Test
    public void testServerDefBuilder()
    {
        try {
            new ThriftServerDefBuilder()
                    .withProcessor(EasyMock.createMock(TProcessor.class))
                    .build();
        }
        catch (Exception e) {
            Assert.fail();
        }
    }
}
