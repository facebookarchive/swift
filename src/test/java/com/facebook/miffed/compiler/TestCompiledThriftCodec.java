/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.compiler;

import com.facebook.miffed.BonkField;
import com.facebook.miffed.metadata.ThriftCatalog;
import com.facebook.miffed.metadata.ThriftStructMetadata;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestCompiledThriftCodec
{
    @Test
    public void testFieldsManual()
            throws Exception
    {
        BonkField bonkField = new BonkField();
        bonkField.message = "message";
        bonkField.type = 42;

        testMetadataBuild(BonkFieldThriftTypeCodec.INSTANCE, bonkField);
    }

    @Test
    public void testFieldsManualGenerator()
            throws Exception
    {
        ThriftTypeCodec<BonkField> codec = new BonkFieldThriftTypeCodecDump(new DynamicClassLoader()).genClass(BonkField.class);

        BonkField bonkField = new BonkField();
        bonkField.message = "message";
        bonkField.type = 42;

        testMetadataBuild(codec, bonkField);
    }

    @Test
    public void testFieldsManualDSL()
            throws Exception
    {
        ThriftTypeCodec<BonkField> codec = new BonkFieldThriftTypeCodecDSL(new DynamicClassLoader()).genClass(BonkField.class);

        BonkField bonkField = new BonkField();
        bonkField.message = "message";
        bonkField.type = 42;

        testMetadataBuild(codec, bonkField);
    }

    private <T> void testMetadataBuild(ThriftTypeCodec<T> codec, T structInstance)
            throws Exception
    {
        Class<T> structClass = (Class<T>) structInstance.getClass();

        ThriftCatalog catalog = new ThriftCatalog();

        ThriftStructMetadata<T> metadata = catalog.getThriftStructMetadata(structClass);
        assertNotNull(metadata);


        TMemoryBuffer transport = new TMemoryBuffer(10 * 1024);
        TCompactProtocol protocol = new TCompactProtocol(transport);
        codec.write(structInstance, new TProtocolWriter(protocol));

        T copy = codec.read(new TProtocolReader(protocol));
        assertNotNull(copy);
        assertEquals(copy, structInstance);
    }
}
