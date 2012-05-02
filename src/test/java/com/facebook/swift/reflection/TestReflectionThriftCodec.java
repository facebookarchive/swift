/* =====================================================================
 *
 * Copyright (c) 2006 Dain Sundstrom.  All rights reserved.
 *
 * =====================================================================
 */
package com.facebook.swift.reflection;

import com.facebook.swift.BonkBean;
import com.facebook.swift.BonkBuilder;
import com.facebook.swift.BonkConstructor;
import com.facebook.swift.BonkField;
import com.facebook.swift.BonkMethod;
import com.facebook.swift.OneOfEverything;
import com.facebook.swift.metadata.ThriftCatalog;
import com.facebook.swift.metadata.ThriftStructMetadata;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestReflectionThriftCodec {
  @Test
  public void testFields() throws Exception {
    BonkField bonkField = new BonkField("message", 42);

    testMetadataBuild(bonkField);
  }

  @Test
  public void testBean() throws Exception {
    BonkBean bonkBean = new BonkBean();
    bonkBean.setMessage("message");
    bonkBean.setType(42);

    testMetadataBuild(bonkBean);
  }

  @Test
  public void testMethod() throws Exception {
    BonkMethod bonkMethod = new BonkMethod();
    bonkMethod.setData("message", 42);

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

    testMetadataBuild(one);
  }

  @Test
  public void testOneOfEverythingFieldEmpty() throws Exception {
    OneOfEverything one = new OneOfEverything();

    testMetadataBuild(one);
  }

  private <T> void testMetadataBuild(T structInstance) throws Exception {
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
