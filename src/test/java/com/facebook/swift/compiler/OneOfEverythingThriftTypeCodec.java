/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.BonkField;
import com.facebook.swift.OneOfEverything;

public class OneOfEverythingThriftTypeCodec implements ThriftTypeCodec<OneOfEverything> {
  public static final OneOfEverythingThriftTypeCodec INSTANCE = 
      new OneOfEverythingThriftTypeCodec();

  public Class<OneOfEverything> getType() {
    return OneOfEverything.class;
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

    protocol.readStructBegin();

    while (protocol.nextField()) {
      switch (protocol.getFieldId()) {
        case 1:
          aBoolean = protocol.readBool();
          break;
        case 2:
          aByte = protocol.readByte();
          break;
        case 3:
          aShort = protocol.readI16();
          break;
        case 4:
          aInt = protocol.readI32();
          break;
        case 5:
          aLong = protocol.readI64();
          break;
        case 6:
          aDouble = protocol.readDouble();
          break;
        case 7:
          aString = protocol.readString();
          break;
        case 8:
          aStruct = protocol.readStruct(BonkFieldThriftTypeCodec.INSTANCE);
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

    return oneOfEverything;
  }

  public void write(OneOfEverything oneOfEverything, TProtocolWriter protocol) throws Exception {
    protocol.writeStructBegin("OneOfEverything");
    protocol.writeBool("aBoolean", (short) 1, oneOfEverything.aBoolean);
    protocol.writeByte("aByte", (short) 2, oneOfEverything.aByte);
    protocol.writeI16("aShort", (short) 3, oneOfEverything.aShort);
    protocol.writeI32("aInt", (short) 4, oneOfEverything.aInt);
    protocol.writeI64("aLong", (short) 5, oneOfEverything.aLong);
    protocol.writeDouble("aDouble", (short) 6, oneOfEverything.aDouble);
    protocol.writeString("aString", (short) 7, oneOfEverything.aString);
    protocol.writeStruct("aStruct", (short) 8, BonkFieldThriftTypeCodec.INSTANCE, oneOfEverything.aStruct);
    protocol.writeStructEnd();
  }
}
