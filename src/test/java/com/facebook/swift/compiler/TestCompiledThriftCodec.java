/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.BonkField;
import com.facebook.swift.OneOfEverything;
import com.facebook.swift.metadata.ThriftCatalog;
import com.facebook.swift.metadata.ThriftStructMetadata;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestCompiledThriftCodec {
  @Test
  public void testFieldsManual() throws Exception {
    ThriftCatalog catalog = new ThriftCatalog();
    ThriftType bonkFieldType = catalog.getThriftType(BonkField.class);
    BonkFieldThriftTypeCodec bonkFieldCodec = new BonkFieldThriftTypeCodec(bonkFieldType);

    BonkField bonkField = new BonkField("message", 42);

    testMetadataBuild(bonkFieldCodec, bonkField);
  }

  @Test
  public void testFieldsAutoGen() throws Exception {
    CompiledThriftCodec compiledThriftCodec = new CompiledThriftCodec(new ThriftCatalog());
    ThriftTypeCodec<BonkField> codec = compiledThriftCodec.getCodec(BonkField.class);

    BonkField bonkField = new BonkField("message", 42);

    testMetadataBuild(codec, bonkField);
  }

  @Test
  public void testOneOfEverythingField() throws Exception {
    CompiledThriftCodec compiledThriftCodec = new CompiledThriftCodec(new ThriftCatalog());
    ThriftTypeCodec<OneOfEverything> codec = compiledThriftCodec.getCodec(OneOfEverything.class);

    OneOfEverything one = new OneOfEverything();
    one.aBoolean = true;
    one.aByte = 11;
    one.aShort = 22;
    one.aInt = 33;
    one.aLong = 44;
    one.aDouble = 55;
    one.aString = "message";
    one.aStruct = new BonkField("struct", 66);

    one.aBooleanSet = ImmutableSet.of(true, false);
    one.aByteSet = ImmutableSet.of((byte)-1, (byte)0, (byte)1);
    one.aShortSet = ImmutableSet.of((short)-1, (short)0, (short)1);
    one.aIntegerSet = ImmutableSet.of(-1, 0, 1);
    one.aLongSet = ImmutableSet.of(-1L, 0L, 1L);
    one.aDoubleSet = ImmutableSet.of(-42.1d, 0.0d, 42.1d);
    one.aStringSet = ImmutableSet.of("a", "string", "set");
    one.aStructSet = ImmutableSet.of(new BonkField("message", 42), new BonkField("other", 11));

    one.aBooleanList = ImmutableList.of(true, false);
    one.aByteList = ImmutableList.of((byte)-1, (byte)0, (byte)1);
    one.aShortList = ImmutableList.of((short)-1, (short)0, (short)1);
    one.aIntegerList = ImmutableList.of(-1, 0, 1);
    one.aLongList = ImmutableList.of(-1L, 0L, 1L);
    one.aDoubleList = ImmutableList.of(-42.1d, 0.0d, 42.1d);
    one.aStringList = ImmutableList.of("a", "string", "list");
    one.aStructList = ImmutableList.of(new BonkField("message", 42), new BonkField("other", 11));

    one.aBooleanMap = ImmutableMap.of("TRUE", true, "FALSE", false);
    one.aByteMap = ImmutableMap.of("-1", (byte)-1, "0", (byte)0, "1", (byte)1);
    one.aShortMap = ImmutableMap.of("-1", (short)-1, "0", (short)0, "1", (short)1);
    one.aIntegerMap = ImmutableMap.of("-1", -1, "0", 0, "1", 1);
    one.aLongMap = ImmutableMap.of("-1", -1L, "0", 0L, "1", 1L);
    one.aDoubleMap = ImmutableMap.of("neg", -42.1d, "0", 0.0d, "pos", 42.1d);
    one.aStringMap = ImmutableMap.of("1", "a", "2", "string", "3", "map");
    one.aStructMap = ImmutableMap.of(
        "main", new BonkField("message", 42),
        "other", new BonkField("other", 11) );

    testMetadataBuild(codec, one);
  }

  @Test
  public void testOneOfEverythingFieldManual() throws Exception {
    ThriftCatalog catalog = new ThriftCatalog();
    ThriftType bonkFieldType = catalog.getThriftType(BonkField.class);
    BonkFieldThriftTypeCodec bonkFieldCodec = new BonkFieldThriftTypeCodec(bonkFieldType);

    ThriftType oneOfEverythingType = catalog.getThriftType(OneOfEverything.class);
    OneOfEverythingThriftTypeCodec codec = new OneOfEverythingThriftTypeCodec(
        oneOfEverythingType,
        bonkFieldCodec,
        new SetThriftTypeCodec<>(new BooleanThriftTypeCodec())
    );

    OneOfEverything one = new OneOfEverything();
    one.aBoolean = true;
    one.aByte = 11;
    one.aShort = 22;
    one.aInt = 33;
    one.aLong = 44;
    one.aDouble = 55;
    one.aString = "message";
    one.aStruct = new BonkField("struct", 66);

    one.aBooleanSet = ImmutableSet.of(true, false);

    testMetadataBuild(codec, one);
  }

  @Test
  public void testOneOfEverythingFieldEmpty() throws Exception {
    CompiledThriftCodec compiledThriftCodec = new CompiledThriftCodec(new ThriftCatalog());
    ThriftTypeCodec<OneOfEverything> codec = compiledThriftCodec.getCodec(OneOfEverything.class);

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
