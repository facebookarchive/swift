/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import com.facebook.swift.ThriftProtocolFieldType;
import com.google.common.base.Preconditions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ThriftType {
  public static final ThriftType BOOL = new ThriftType(ThriftProtocolFieldType.BOOL);
  public static final ThriftType BYTE = new ThriftType(ThriftProtocolFieldType.BYTE);
  public static final ThriftType DOUBLE = new ThriftType(ThriftProtocolFieldType.DOUBLE);
  public static final ThriftType I16 = new ThriftType(ThriftProtocolFieldType.I16);
  public static final ThriftType I32 = new ThriftType(ThriftProtocolFieldType.I32);
  public static final ThriftType I64 = new ThriftType(ThriftProtocolFieldType.I64);
  public static final ThriftType STRING = new ThriftType(ThriftProtocolFieldType.STRING);

  public static ThriftType struct(ThriftStructMetadata<?> structMetadata) {
    return new ThriftType(structMetadata);
  }

  public static ThriftType map(ThriftType keyType, ThriftType valueType) {
    checkNotNull(keyType, "keyType is null");
    checkNotNull(valueType, "valueType is null");
    return new ThriftType(ThriftProtocolFieldType.MAP, keyType, valueType);
  }

  public static ThriftType set(ThriftType valueType) {
    checkNotNull(valueType, "valueType is null");
    return new ThriftType(ThriftProtocolFieldType.SET, null, valueType);
  }

  public static ThriftType list(ThriftType valueType) {
    checkNotNull(valueType, "valueType is null");
    return new ThriftType(ThriftProtocolFieldType.LIST, null, valueType);
  }

  // public static final ThriftType ENUM = new ThriftFieldType(ThriftProtocolFieldType.ENUM);

  private final ThriftProtocolFieldType protocolType;
  private final ThriftType keyType;
  private final ThriftType valueType;
  private final ThriftStructMetadata<?> structMetadata;

  private ThriftType(ThriftProtocolFieldType protocolType) {
    this.protocolType = protocolType;
    keyType = null;
    valueType = null;
    structMetadata = null;
  }

  private ThriftType(
    ThriftProtocolFieldType protocolType,
    ThriftType keyType,
    ThriftType valueType
  ) {
    this.protocolType = protocolType;
    this.keyType = keyType;
    this.valueType = valueType;
    structMetadata = null;
  }

  private ThriftType(ThriftStructMetadata<?> structMetadata) {
    this.protocolType = ThriftProtocolFieldType.STRUCT;
    keyType = null;
    valueType = null;
    this.structMetadata = structMetadata;
  }

  public ThriftProtocolFieldType getProtocolType() {
    return protocolType;
  }

  public ThriftType getKeyType() {
    checkState(keyType != null, "%s does not have a key", protocolType);
    return keyType;
  }

  public ThriftType getValueType() {
    checkState(valueType != null, "%s does not have a value", protocolType);
    return valueType;
  }

  public ThriftStructMetadata<?> getStructMetadata() {
    checkState(structMetadata != null, "%s does not have struct metadata", protocolType);
    return structMetadata;
  }
}
