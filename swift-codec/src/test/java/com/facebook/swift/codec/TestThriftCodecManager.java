/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.codec;

import com.facebook.swift.codec.internal.EnumThriftCodec;
import com.facebook.swift.codec.internal.ThriftCodecFactory;
import com.facebook.swift.codec.internal.coercion.DefaultJavaCoercions;
import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftEnumMetadata;
import com.facebook.swift.codec.metadata.ThriftEnumMetadataBuilder;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

import static com.facebook.swift.codec.metadata.ThriftType.BOOL;
import static com.facebook.swift.codec.metadata.ThriftType.BYTE;
import static com.facebook.swift.codec.metadata.ThriftType.DOUBLE;
import static com.facebook.swift.codec.metadata.ThriftType.I16;
import static com.facebook.swift.codec.metadata.ThriftType.I32;
import static com.facebook.swift.codec.metadata.ThriftType.I64;
import static com.facebook.swift.codec.metadata.ThriftType.STRING;
import static com.facebook.swift.codec.metadata.ThriftType.enumType;
import static com.facebook.swift.codec.metadata.ThriftType.list;
import static com.facebook.swift.codec.metadata.ThriftType.map;
import static com.facebook.swift.codec.metadata.ThriftType.set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class TestThriftCodecManager
{
    public static final String UTF8_TEST_STRING = "A" + "\u00ea" + "\u00f1" + "\u00fc" + "C";
    private ThriftCodecManager codecManager;

    @BeforeMethod
    protected void setUp()
            throws Exception
    {
        codecManager = new ThriftCodecManager(new ThriftCodecFactory()
        {
            @Override
            public ThriftCodec<?> generateThriftTypeCodec(ThriftCodecManager codecManager, ThriftStructMetadata metadata)
            {
                throw new UnsupportedOperationException();
            }
        });
        ThriftCatalog catalog = codecManager.getCatalog();
        catalog.addDefaultCoercions(DefaultJavaCoercions.class);
        ThriftType fruitType = catalog.getThriftType(Fruit.class);
        codecManager.addCodec(new EnumThriftCodec<Fruit>(fruitType));
    }

    @Test
    public void testBasicTypes()
            throws Exception
    {
        testRoundTripSerialize(true);
        testRoundTripSerialize(false);
        testRoundTripSerialize((byte) 100);
        testRoundTripSerialize((short) 1000);
        testRoundTripSerialize(10000);
        testRoundTripSerialize((long) 10000000);
        testRoundTripSerialize(42.42d);
        testRoundTripSerialize("some string");
        testRoundTripSerialize(UTF8_TEST_STRING);

        testRoundTripSerialize("some string", new TJSONProtocol.Factory());
        testRoundTripSerialize(UTF8_TEST_STRING, new TJSONProtocol.Factory());
    }

    @Test
    public void testBasicThriftTypes()
            throws Exception
    {
        testRoundTripSerialize(BOOL, true);
        testRoundTripSerialize(BOOL, false);
        testRoundTripSerialize(BYTE, (byte) 100);
        testRoundTripSerialize(I16, (short) 1000);
        testRoundTripSerialize(I32, 10000);
        testRoundTripSerialize(I64, (long) 10000000);
        testRoundTripSerialize(DOUBLE, 42.42d);
        testRoundTripSerialize(STRING, "some string");
        testRoundTripSerialize(STRING, UTF8_TEST_STRING);

        testRoundTripSerialize(STRING, "some string", new TJSONProtocol.Factory());
        testRoundTripSerialize(STRING, UTF8_TEST_STRING, new TJSONProtocol.Factory());
    }

    @Test
    public void testEnum()
            throws Exception
    {
        ThriftEnumMetadata<Fruit> fruitEnumMetadata = new ThriftEnumMetadataBuilder<>(Fruit.class).build();
        ThriftEnumMetadata<Letter> letterEnumMetadata = new ThriftEnumMetadataBuilder<>(Letter.class).build();
        testRoundTripSerialize(Fruit.CHERRY);
        testRoundTripSerialize(Letter.C);
        testRoundTripSerialize(enumType(fruitEnumMetadata), Fruit.CHERRY);
        testRoundTripSerialize(enumType(letterEnumMetadata), Letter.C);
        testRoundTripSerialize(list(enumType(fruitEnumMetadata)), ImmutableList.copyOf(Fruit.values()));
        testRoundTripSerialize(list(enumType(letterEnumMetadata)), ImmutableList.copyOf(Letter.values()));
    }

    @Test
    public void testCollectionThriftTypes()
            throws Exception
    {
        testRoundTripSerialize(set(STRING), ImmutableSet.of("some string", "another string"));
        testRoundTripSerialize(list(STRING), ImmutableList.of("some string", "another string"));
        testRoundTripSerialize(map(STRING, STRING), ImmutableMap.of("1", "one", "2", "two"));
    }

    @Test
    public void testCoercedCollection()
            throws Exception
    {
        testRoundTripSerialize(set(I32.coerceTo(Integer.class)), ImmutableSet.of(1, 2, 3));
        testRoundTripSerialize(list(I32.coerceTo(Integer.class)), ImmutableList.of(4, 5, 6));
        testRoundTripSerialize(map(I32.coerceTo(Integer.class), I32.coerceTo(Integer.class)), ImmutableMap.of(1, 2, 2, 4, 3, 9));
    }

    @Test
    public void testAddStructCodec()
            throws Exception
    {
        BonkField bonk = new BonkField("message", 42);

        // no codec for BonkField so this will fail
        try {
            testRoundTripSerialize(bonk);
            fail("Expected exception");
        }
        catch (Exception ignored) {
        }

        // add the codec
        ThriftType type = codecManager.getCatalog().getThriftType(BonkField.class);
        codecManager.addCodec(new BonkFieldThriftCodec(type));

        // try again
        testRoundTripSerialize(bonk);
        testRoundTripSerialize(bonk, new TJSONProtocol.Factory());
    }

    @Test
    public void testAddUnionCodec()
            throws Exception
    {
        UnionField union = new UnionField();
        union._id= 1;
        union.stringValue = "Hello, World";

        // no codec for UnionField so this will fail
        try {
            testRoundTripSerialize(union);
            fail("Expected exception");
        }
        catch (Exception ignored) {
        }

        // add the codec
        ThriftType type = codecManager.getCatalog().getThriftType(UnionField.class);
        codecManager.addCodec(new UnionFieldThriftCodec(type, codecManager.getCodec(Fruit.class)));

        // try again
        testRoundTripSerialize(union);

        union = new UnionField();
        union._id = 2;
        union.longValue = 4815162342L;

        testRoundTripSerialize(union);

        union = new UnionField();
        union._id = 3;
        union.fruitValue = Fruit.BANANA;

        testRoundTripSerialize(union);
        testRoundTripSerialize(union, new TJSONProtocol.Factory());
    }

    private <T> void testRoundTripSerialize(T value)
            throws Exception
    {
        testRoundTripSerialize(value, new TCompactProtocol.Factory());
    }

    private <T> void testRoundTripSerialize(T value, TProtocolFactory protocolFactory)
            throws Exception
    {
        testRoundTripSerialize(codecManager.getCatalog().getThriftType(value.getClass()), value, new TCompactProtocol.Factory());
    }

    private <T> void testRoundTripSerialize(ThriftType type, T value)
            throws Exception
    {
        testRoundTripSerialize(type, value, new TCompactProtocol.Factory());
    }

    private <T> void testRoundTripSerialize(ThriftType type, T value, TProtocolFactory protocolFactory)
            throws Exception
    {
        // write value
        TMemoryBuffer transport = new TMemoryBuffer(10 * 1024);
        TProtocol protocol = protocolFactory.getProtocol(transport);
        codecManager.write(type, value, protocol);

        // read value back
        T copy = (T) codecManager.read(type, protocol);
        assertNotNull(copy);

        // verify they are the same
        assertEquals(copy, value);
    }

    public void testWriteToBuffer() {
        SecureRandom random = new SecureRandom();
        BasicThriftStruct tstruct = new BasicThriftStruct(
            new BigInteger(130, random).toString(),
            new BigInteger(130, random).toString(),
            new BigInteger(130, random).toString(),
            1337L);
        TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        codecManager.write(BasicThriftStruct.class, oStream, protocolFactory);

        BasicThriftStruct tstructCopy = codecManager.read(
                oStream.toByteArray(),
                BasicThriftStruct.class,
                protocolFactory);

        Assert.assertEquals(tstruct, tstructCopy);
    }
}
