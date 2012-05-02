/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.BonkBean;
import com.facebook.swift.BonkBuilder;
import com.facebook.swift.BonkConstructor;
import com.facebook.swift.BonkField;
import com.facebook.swift.BonkMethod;
import com.facebook.swift.OneOfEverything;
import com.facebook.swift.metadata.ThriftCatalog;
import com.facebook.swift.metadata.ThriftStructMetadata;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
  public void testFields() throws Exception {
    BonkField bonkField = new BonkField("message", 42);
    testMetadataBuild(bonkField);
  }

  @Test
  public void testBean() throws Exception {
    BonkBean bonkBean = new BonkBean("message", 42);
    testMetadataBuild(bonkBean);
  }

  @Test
  public void testMethod() throws Exception {
    BonkMethod bonkMethod = new BonkMethod("message", 42);
    testMetadataBuild(bonkMethod);
  }

  @Test
  public void testConstructor() throws Exception {
    BonkConstructor bonkConstructor = new BonkConstructor("message", 42);
    testMetadataBuild(bonkConstructor);
  }

  @Test
  public void testBuilder() throws Exception {
    BonkBuilder bonkBuilder = new BonkBuilder("message", 42);
    testMetadataBuild(bonkBuilder);
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

    one.aBooleanValueMap = ImmutableMap.of("TRUE", true, "FALSE", false);
    one.aByteValueMap = ImmutableMap.of("-1", (byte)-1, "0", (byte)0, "1", (byte)1);
    one.aShortValueMap = ImmutableMap.of("-1", (short)-1, "0", (short)0, "1", (short)1);
    one.aIntegerValueMap = ImmutableMap.of("-1", -1, "0", 0, "1", 1);
    one.aLongValueMap = ImmutableMap.of("-1", -1L, "0", 0L, "1", 1L);
    one.aDoubleValueMap = ImmutableMap.of("neg", -42.1d, "0", 0.0d, "pos", 42.1d);
    one.aStringValueMap = ImmutableMap.of("1", "a", "2", "string", "3", "map");
    one.aStructValueMap = ImmutableMap.of(
        "main", new BonkField("message", 42),
        "other", new BonkField("other", 11) );

    one.aBooleanKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aBooleanValueMap).inverse());
    one.aByteKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aByteValueMap).inverse());
    one.aShortKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aShortValueMap).inverse());
    one.aIntegerKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aIntegerValueMap).inverse());
    one.aLongKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aLongValueMap).inverse());
    one.aDoubleKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aDoubleValueMap).inverse());
    one.aStringKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aStringValueMap).inverse());
    one.aStructKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aStructValueMap).inverse());

    one.aSetOfListsOfMaps = ImmutableSet.<List<Map<String, BonkField>>>of(
        ImmutableList.<Map<String, BonkField>>of(
            ImmutableMap.of(
                "1: main", new BonkField("message", 42),
                "1: other", new BonkField("other", 11)
            ),
            ImmutableMap.of(
                "1: main", new BonkField("message", 42),
                "1: other", new BonkField("other", 11)
            )
        ),
        ImmutableList.<Map<String, BonkField>>of(
            ImmutableMap.of(
                "2: main", new BonkField("message", 42),
                "2: other", new BonkField("other", 11)
            ),
            ImmutableMap.of(
                "2: main", new BonkField("message", 42),
                "2: other", new BonkField("other", 11)
            )
        )
    );

    one.aMapOfListToSet = ImmutableMap.<List<String>, Set<BonkField>>of(
        ImmutableList.of("a", "b"),
        ImmutableSet.of(
            new BonkField("1: message", 42),
            new BonkField("1: other", 11)
        ),
        ImmutableList.of("c", "d"),
        ImmutableSet.of(
            new BonkField("2: message", 42),
            new BonkField("2: other", 11)
        )
    );

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

  private <T> void testMetadataBuild(T value) throws Exception {
    CompiledThriftCodec compiledThriftCodec = new CompiledThriftCodec(new ThriftCatalog());
    ThriftTypeCodec<T> codec = (ThriftTypeCodec<T>) compiledThriftCodec.getCodec(value.getClass());

    testMetadataBuild(codec, value);
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
