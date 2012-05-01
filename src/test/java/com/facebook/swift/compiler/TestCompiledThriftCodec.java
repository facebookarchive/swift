/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.BonkConstructor;
import com.facebook.swift.BonkField;
import com.facebook.swift.OneOfEverything;
import com.facebook.swift.metadata.ThriftCatalog;
import com.facebook.swift.metadata.ThriftStructMetadata;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestCompiledThriftCodec {
  @Test
  public void testFieldsManual()
    throws Exception {
    BonkField bonkField = new BonkField();
    bonkField.message = "message";
    bonkField.type = 42;

    testMetadataBuild(BonkFieldThriftTypeCodec.INSTANCE, bonkField);
  }

  @Test
  public void testFieldsManualDSL() throws Exception {
    ThriftTypeCodec<BonkField> codec = new BonkFieldThriftTypeCodecDSL(new DynamicClassLoader())
      .genClass( BonkField.class);

    BonkField bonkField = new BonkField();
    bonkField.message = "message";
    bonkField.type = 42;

    testMetadataBuild(codec, bonkField);
  }

  @Test
  public void testFieldsAutoGen() throws Exception {
    CompiledThriftCodec compiledThriftCodec = new CompiledThriftCodec(new ThriftCatalog());
    ThriftTypeCodec<BonkField> codec = compiledThriftCodec.getTypeCodec(BonkField.class);

    BonkField bonkField = new BonkField();
    bonkField.message = "message";
    bonkField.type = 42;

    testMetadataBuild(codec, bonkField);
  }

  @Test
  public void testOneOfEverythingField() throws Exception {
    CompiledThriftCodec compiledThriftCodec = new CompiledThriftCodec(new ThriftCatalog());
    ThriftTypeCodec<OneOfEverything> codec = compiledThriftCodec.getTypeCodec(OneOfEverything.class);

    OneOfEverything one = new OneOfEverything();
    one.aBoolean = true;
    one.aByte = 11;
    one.aShort = 22;
    one.aInt = 33;
    one.aLong = 44;
    one.aDouble = 55;
    one.aString = "message";
    one.aStruct = new BonkField();
    one.aStruct.message = "struct";
    one.aStruct.type = 66;

    testMetadataBuild(codec, one);
  }

  @Test
  public void testOneOfEverythingFieldManual() throws Exception {
    OneOfEverythingThriftTypeCodec codec = OneOfEverythingThriftTypeCodec.INSTANCE;

    OneOfEverything one = new OneOfEverything();
    one.aBoolean = true;
    one.aByte = 11;
    one.aShort = 22;
    one.aInt = 33;
    one.aLong = 44;
    one.aDouble = 55;
    one.aString = "message";
    one.aStruct = new BonkField();
    one.aStruct.message = "struct";
    one.aStruct.type = 66;

    testMetadataBuild(codec, one);
  }

  @Test
  public void testOneOfEverythingFieldEmpty() throws Exception {
    CompiledThriftCodec compiledThriftCodec = new CompiledThriftCodec(new ThriftCatalog());
    ThriftTypeCodec<OneOfEverything> codec = compiledThriftCodec.getTypeCodec(OneOfEverything.class);

    OneOfEverything one = new OneOfEverything();
    testMetadataBuild(codec, one);
  }

  private <T> void testMetadataBuild(ThriftTypeCodec<T> codec, T structInstance) throws Exception {
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
