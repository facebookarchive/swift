/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift;

import com.facebook.swift.coercion.GeneralJavaCoercions;
import com.facebook.swift.internal.ThriftCodecFactory;
import com.facebook.swift.metadata.ThriftStructMetadata;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.facebook.swift.metadata.ThriftType.*;
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
    codecManager.getCatalog().addGeneralCoercions(GeneralJavaCoercions.class);
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
    testRoundTripSerialize(STRING, "some string");
  }

  @Test
  public void testCollectionThriftTypes() throws Exception {
    testRoundTripSerialize(set(STRING), ImmutableSet.of("some string", "another string"));
    testRoundTripSerialize(list(STRING), ImmutableList.of("some string", "another string"));
    testRoundTripSerialize(map(I32, STRING), ImmutableMap.of(1, "one", 2, "two"));
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
}
