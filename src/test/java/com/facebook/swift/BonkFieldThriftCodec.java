/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift;

import com.facebook.swift.internal.TProtocolReader;
import com.facebook.swift.internal.TProtocolWriter;
import com.facebook.swift.metadata.ThriftType;

import static com.facebook.swift.coercion.DefaultJavaCoercions.byteBufferToString;

public class BonkFieldThriftCodec implements ThriftCodec<BonkField> {

  private final ThriftType type;

  public BonkFieldThriftCodec(ThriftType type) {
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
          message = byteBufferToString(protocol.readBinaryField());
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
    if (message != null) {
      bonkField.message = message;
    }
    bonkField.type = type;

    return bonkField;
  }

  public void write(BonkField value, TProtocolWriter protocol) throws Exception {
    protocol.writeStructBegin("bonk");

    String message = value.message;
    if (message != null) {
      protocol.writeStringField("message", (short) 1, message);
    }

    protocol.writeI32Field("type", (short) 2, value.type);
    protocol.writeStructEnd();
  }
}
