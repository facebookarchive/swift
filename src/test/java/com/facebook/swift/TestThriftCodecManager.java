/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift;

import com.facebook.swift.coercion.DefaultJavaCoercions;
import com.facebook.swift.internal.ThriftCodecFactory;
import com.facebook.swift.metadata.ThriftEnumMetadata;
import com.facebook.swift.metadata.ThriftStructMetadata;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;

import static com.facebook.swift.metadata.ThriftType.*;
import static com.google.common.base.Charsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class TestThriftCodecManager {

  private ThriftCodecManager codecManager;

  @BeforeMethod
  protected void setUp() throws Exception {
    codecManager = new ThriftCodecManager(
        new ThriftCodecFactory() {
          @Override
          public <T> ThriftCodec<T> generateThriftTypeCodec(
              ThriftCodecManager codecManager, ThriftStructMetadata<T> metadata
          ) {
            throw new UnsupportedOperationException();
          }
        }
    );
    codecManager.getCatalog().addDefaultCoercions(DefaultJavaCoercions.class);
  }

  @Test
  public void testBasicTypes() throws Exception {
    testRoundTripSerialize(true);
    testRoundTripSerialize(false);
    testRoundTripSerialize((byte) 100);
    testRoundTripSerialize((short) 1000);
    testRoundTripSerialize(10000);
    testRoundTripSerialize((long) 10000000);
    testRoundTripSerialize(42.42d);
    testRoundTripSerialize("some string");
  }

  @Test
  public void testBasicThriftTypes() throws Exception {
    testRoundTripSerialize(BOOL, true);
    testRoundTripSerialize(BOOL, false);
    testRoundTripSerialize(BYTE, (byte) 100);
    testRoundTripSerialize(I16, (short) 1000);
    testRoundTripSerialize(I32, 10000);
    testRoundTripSerialize(I64, (long) 10000000);
    testRoundTripSerialize(DOUBLE, 42.42d);
    testRoundTripSerialize(STRING, toByteBuffer("some string"));
  }

  @Test
  public void testEnum() throws Exception {
    ThriftEnumMetadata<Fruit> fruitEnumMetadata = new ThriftEnumMetadata<>(Fruit.class);
    ThriftEnumMetadata<Letter> letterEnumMetadata = new ThriftEnumMetadata<>(Letter.class);
    testRoundTripSerialize(Fruit.CHERRY);
    testRoundTripSerialize(Letter.C);
    testRoundTripSerialize(enumType(fruitEnumMetadata), Fruit.CHERRY);
    testRoundTripSerialize(enumType(letterEnumMetadata), Letter.C);
    testRoundTripSerialize(list(enumType(fruitEnumMetadata)), ImmutableList.copyOf(Fruit.values()));
    testRoundTripSerialize(
        list(enumType(letterEnumMetadata)),
        ImmutableList.copyOf(Letter.values())
    );
  }

  @Test
  public void testCollectionThriftTypes() throws Exception {
    testRoundTripSerialize(set(STRING), ImmutableSet.of(
        toByteBuffer("some string"),
        toByteBuffer("another string")
    ) );
    testRoundTripSerialize(list(STRING), ImmutableList.of(
        toByteBuffer("some string"),
        toByteBuffer("another string")
    ));
    testRoundTripSerialize(map(STRING, STRING), ImmutableMap.of(
        toByteBuffer("1"), toByteBuffer("one"),
        toByteBuffer("2"), toByteBuffer("two")
    ));
  }

  @Test
  public void testCoercedCollection() throws Exception {
    testRoundTripSerialize(
        set(I32.coerceTo(Integer.class)),
        ImmutableSet.of(1, 2, 3)
    );
    testRoundTripSerialize(
        list(I32.coerceTo(Integer.class)),
        ImmutableList.of(4, 5, 6)
    );
    testRoundTripSerialize(
        map(I32.coerceTo(Integer.class), I32.coerceTo(Integer.class)),
        ImmutableMap.of(1, 2, 2, 4, 3, 9)
    );
  }

  @Test
  public void testAddCodec() throws Exception {
    BonkField bonk = new BonkField("message", 42);

    // no codec for BonkField so this will fail
    try {
      testRoundTripSerialize(bonk);
      fail("Expected exception");
    } catch (Exception ignored) {
    }

    // add the codec
    ThriftType type = codecManager.getCatalog().getThriftType(BonkField.class);
    codecManager.addCodec(new BonkFieldThriftCodec(type));

    // try again
    testRoundTripSerialize(bonk);
  }

  private <T> void testRoundTripSerialize(T value) throws Exception {
    // write value
    TMemoryBuffer transport = new TMemoryBuffer(10 * 1024);
    TCompactProtocol protocol = new TCompactProtocol(transport);
    codecManager.write((Class<T>) value.getClass(), value, protocol);

    // read value back
    T copy = codecManager.read((Class<T>) value.getClass(), protocol);
    assertNotNull(copy);

    // verify they are the same
    assertEquals(copy, value);
  }

  private <T> void testRoundTripSerialize(ThriftType type, T value) throws Exception {
    // write value
    TMemoryBuffer transport = new TMemoryBuffer(10 * 1024);
    TCompactProtocol protocol = new TCompactProtocol(transport);
    codecManager.write(type, value, protocol);

    // read value back
    T copy = (T) codecManager.read(type, protocol);
    assertNotNull(copy);

    // verify they are the same
    assertEquals(copy, value);
  }
  
  private ByteBuffer toByteBuffer(String string) {
    return ByteBuffer.wrap(string.getBytes(UTF_8));
  }  
}
