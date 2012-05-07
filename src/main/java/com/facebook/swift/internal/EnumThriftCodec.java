/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.internal;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftEnumMetadata;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.base.Preconditions;

/**
 * EnumThriftCodec is a codec for Java enum types.  An enum is encoded as an I32 in Thrift, and this
 * class handles converting this vale to a Java enum constant.
 */
public class EnumThriftCodec<T extends Enum<T>> implements ThriftCodec<T> {
  private final ThriftType type;
  private final ThriftEnumMetadata<T> enumMetadata;

  public EnumThriftCodec(ThriftType type) {
    this.type = type;
    enumMetadata = (ThriftEnumMetadata<T>) type.getEnumMetadata();
  }

  @Override
  public ThriftType getType() {
    return type;
  }

  @Override
  public T read(TProtocolReader protocol) throws Exception {
    int enumValue = protocol.readI32();
    if (enumValue >= 0) {
      if (enumMetadata.hasExplicitThriftValue()) {
        T enumConstant = enumMetadata.getByEnumValue().get(enumValue);
        if (enumConstant != null) {
          return enumConstant;
        }
      } else {
        T[] enumConstants = enumMetadata.getEnumClass().getEnumConstants();
        if (enumValue < enumConstants.length) {
          return enumConstants[enumValue];
        }
      }
    }
    throw new IllegalAccessException(
        String.format("Enum %s does not have a value for %s",
        enumMetadata.getEnumClass(),
        enumValue));
  }

  @Override
  public void write(T enumConstant, TProtocolWriter protocol) throws Exception {
    Preconditions.checkNotNull(enumConstant, "enumConstant is null");

    int enumValue;
    if (enumMetadata.hasExplicitThriftValue()) {
      enumValue = enumMetadata.getByEnumConstant().get(enumConstant);
    } else {
      enumValue = enumConstant.ordinal();
    }
    protocol.writeI32(enumValue);
  }
}
