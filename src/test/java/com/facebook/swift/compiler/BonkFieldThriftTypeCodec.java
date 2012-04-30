/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.BonkField;

public class BonkFieldThriftTypeCodec implements ThriftTypeCodec<BonkField> {
  public static final BonkFieldThriftTypeCodec INSTANCE = new BonkFieldThriftTypeCodec();

  public Class<BonkField> getType() {
    return BonkField.class;
  }

  public BonkField read(TProtocolReader protocol) throws Exception {
    String message = null;
    int type = 0;

    protocol.readStructBegin();

    while (protocol.nextField()) {
      switch (protocol.getFieldId()) {
        case 1:
          message = protocol.readString();
          break;
        case 2:
          type = protocol.readI32();
          break;
        default:
          protocol.skipFieldData();
      }
    }
    protocol.readStructEnd();

    BonkField bonkField = new BonkField();
    bonkField.message = message;
    bonkField.type = type;

    return bonkField;
  }

  public void write(BonkField value, TProtocolWriter protocol) throws Exception {
    protocol.writeStructBegin("bonk");
    protocol.writeString("message", (short) 1, value.message);
    protocol.writeI32("type", (short) 2, value.type);
    protocol.writeStructEnd();
  }
}
