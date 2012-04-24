/* =====================================================================
 *
 * Copyright (c) 2006 Dain Sundstrom.  All rights reserved.
 *
 * =====================================================================
 */
package com.facebook.miffed.reflection;

import com.facebook.miffed.BonkBean;
import com.facebook.miffed.BonkConstructor;
import com.facebook.miffed.BonkField;
import com.facebook.miffed.metadata.ThriftCatalog;
import com.facebook.miffed.metadata.ThriftStructMetadata;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestReflectionCodec
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
    public void testConstructor()
            throws Exception
    {
        BonkConstructor bonkConstructor = new BonkConstructor("message", 42);
        testMetadataBuild(bonkConstructor);
    }

    private <T> void testMetadataBuild(T structInstance)
            throws Exception
    {
        Class<T> structClass = (Class<T>) structInstance.getClass();

        ThriftCatalog catalog = new ThriftCatalog();

        ThriftStructMetadata<T> metadata = catalog.getThriftStructMetadata(structClass);
        assertNotNull(metadata);


        ReflectionCodec codec = new ReflectionCodec();
        TMemoryBuffer transport = new TMemoryBuffer(10 * 1024);
        TCompactProtocol protocol = new TCompactProtocol(transport);
        codec.writeStruct(metadata, structInstance, protocol);

        T copy = codec.read(metadata, protocol);
        assertNotNull(copy);
        assertEquals(copy, structInstance);
    }
}
