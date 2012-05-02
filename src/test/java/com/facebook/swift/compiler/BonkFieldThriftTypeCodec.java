/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.BonkField;
import com.facebook.swift.metadata.ThriftType;

public class BonkFieldThriftTypeCodec implements ThriftTypeCodec<BonkField> {

  private final ThriftType type;

  public BonkFieldThriftTypeCodec(ThriftType type) {
    this.type = type;
  }

  @Override
  public ThriftType getType() {
    return type;
  }

  public BonkField read(TProtocolReader protocol) throws Exception {
    String message = null;
    int type = 0;

    protocol.readStructBegin();

    while (protocol.nextField()) {
      switch (protocol.getFieldId()) {
        case 1:
          message = protocol.readStringField();
          break;
        case 2:
          type = protocol.readI32Field();
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
    protocol.writeStringField("message", (short) 1, value.message);
    protocol.writeI32Field("type", (short) 2, value.type);
    protocol.writeStructEnd();
  }
}
