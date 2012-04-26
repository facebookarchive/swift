/* =====================================================================
 *
 * Copyright (c) 2006 Dain Sundstrom.  All rights reserved.
 *
 * =====================================================================
 */
package com.facebook.miffed.reflection;

import com.facebook.miffed.BonkBean;
import com.facebook.miffed.BonkBuilder;
import com.facebook.miffed.BonkConstructor;
import com.facebook.miffed.BonkField;
import com.facebook.miffed.BonkMethod;
import com.facebook.miffed.metadata.ThriftCatalog;
import com.facebook.miffed.metadata.ThriftStructMetadata;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestReflectionThriftCodec
{
    @Test
    public void testFields()
            throws Exception
    {
        BonkField bonkField = new BonkField();
        bonkField.message = "message";
        bonkField.type = 42;

        testMetadataBuild(bonkField);
    }

    @Test
    public void testBean()
            throws Exception
    {
        BonkBean bonkBean = new BonkBean();
        bonkBean.setMessage("message");
        bonkBean.setType(42);

        testMetadataBuild(bonkBean);
    }

    @Test
    public void testMethod()
            throws Exception
    {
        BonkMethod bonkMethod = new BonkMethod();
        bonkMethod.setData("message", 42);

        testMetadataBuild(bonkMethod);
    }

    @Test
    public void testConstructor()
            throws Exception
    {
        BonkConstructor bonkConstructor = new BonkConstructor("message", 42);
        testMetadataBuild(bonkConstructor);
    }

    @Test
    public void testBuilder()
            throws Exception
    {
        BonkBuilder bonkBuilder = new BonkBuilder("message", 42);
        testMetadataBuild(bonkBuilder);
    }

    private <T> void testMetadataBuild(T structInstance)
            throws Exception
    {
        Class<T> structClass = (Class<T>) structInstance.getClass();

        ThriftCatalog catalog = new ThriftCatalog();

        ThriftStructMetadata<T> metadata = catalog.getThriftStructMetadata(structClass);
        assertNotNull(metadata);


        ReflectionThriftCodec codec = new ReflectionThriftCodec(catalog);
        TMemoryBuffer transport = new TMemoryBuffer(10 * 1024);
        TCompactProtocol protocol = new TCompactProtocol(transport);
        codec.write(structClass, structInstance, protocol);

        T copy = codec.read(structClass, protocol);
        assertNotNull(copy);
        assertEquals(copy, structInstance);
    }
}
