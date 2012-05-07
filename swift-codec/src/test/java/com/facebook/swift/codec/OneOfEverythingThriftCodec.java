/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftType;

import java.util.Set;

public class OneOfEverythingThriftCodec implements ThriftCodec<OneOfEverything> {

  private final ThriftType type;
  private final ThriftCodec<BonkField> aStructCodec;
  private final ThriftCodec<Set<Boolean>> aBooleanSetCodec;

  public OneOfEverythingThriftCodec(
      ThriftType type,
      ThriftCodec<BonkField> aStructCodec,
      ThriftCodec<Set<Boolean>> aBooleanSetCodec
  ) {
    this.type = type;
    this.aStructCodec = aStructCodec;
    this.aBooleanSetCodec = aBooleanSetCodec;
  }

  @Override
  public ThriftType getType() {
    return type;
  }

  public OneOfEverything read(TProtocolReader protocol) throws Exception {
    boolean aBoolean = false;
    byte aByte = 0;
    short aShort = 0;
    int aInt = 0;
    long aLong = 0;
    double aDouble = 0;
    String aString = null;
    BonkField aStruct = null;
    Set<Boolean> aBooleanSet = null;

    protocol.readStructBegin();

    while (protocol.nextField()) {
      switch (protocol.getFieldId()) {
        case 1:
          aBoolean = protocol.readBoolField();
          break;
        case 2:
          aByte = protocol.readByteField();
          break;
        case 3:
          aShort = protocol.readI16Field();
          break;
        case 4:
          aInt = protocol.readI32Field();
          break;
        case 5:
          aLong = protocol.readI64Field();
          break;
        case 6:
          aDouble = protocol.readDoubleField();
          break;
        case 7:
          aString = protocol.readStringField();
          break;
        case 8:
          aStruct = protocol.readStructField(aStructCodec);
          break;
        case 9:
          aBooleanSet = protocol.readSetField(aBooleanSetCodec);
          break;
        default:
          protocol.skipFieldData();
      }
    }
    protocol.readStructEnd();

    OneOfEverything oneOfEverything = new OneOfEverything();
    oneOfEverything.aBoolean = aBoolean;
    oneOfEverything.aByte = aByte;
    oneOfEverything.aShort = aShort;
    oneOfEverything.aInt = aInt;
    oneOfEverything.aLong = aLong;
    oneOfEverything.aDouble = aDouble;
    oneOfEverything.aString = aString;
    oneOfEverything.aStruct = aStruct;
    oneOfEverything.aBooleanSet = aBooleanSet;

    return oneOfEverything;
  }

  public void write(OneOfEverything oneOfEverything, TProtocolWriter protocol) throws Exception {
    protocol.writeStructBegin("OneOfEverything");
    protocol.writeBoolField("aBoolean", (short) 1, oneOfEverything.aBoolean);
    protocol.writeByteField("aByte", (short) 2, oneOfEverything.aByte);
    protocol.writeI16Field("aShort", (short) 3, oneOfEverything.aShort);
    protocol.writeI32Field("aInt", (short) 4, oneOfEverything.aInt);
    protocol.writeI64Field("aLong", (short) 5, oneOfEverything.aLong);
    protocol.writeDoubleField("aDouble", (short) 6, oneOfEverything.aDouble);
    protocol.writeStringField("aString", (short) 7, oneOfEverything.aString);
    protocol.writeStructField("aStruct", (short) 8, aStructCodec, oneOfEverything.aStruct);
    protocol.writeSetField("aBooleanSet", (short) 9, aBooleanSetCodec, oneOfEverything.aBooleanSet);
    protocol.writeStructEnd();
  }
}
