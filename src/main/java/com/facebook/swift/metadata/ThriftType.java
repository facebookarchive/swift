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
    Preconditions.checkNotNull(protocolType, "protocolType is null");

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
    Preconditions.checkNotNull(protocolType, "protocolType is null");
    Preconditions.checkNotNull(valueType, "valueType is null");

    this.protocolType = protocolType;
    this.keyType = keyType;
    this.valueType = valueType;
    this.structMetadata = null;
  }

  private ThriftType(ThriftStructMetadata<?> structMetadata) {
    Preconditions.checkNotNull(structMetadata, "structMetadata is null");

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ThriftType that = (ThriftType) o;

    if (keyType != null ? !keyType.equals(that.keyType) : that.keyType != null) {
      return false;
    }
    if (protocolType != that.protocolType) {
      return false;
    }
    if (structMetadata != null ? !structMetadata.equals(that.structMetadata) : that.structMetadata != null) {
      return false;
    }
    if (valueType != null ? !valueType.equals(that.valueType) : that.valueType != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = protocolType.hashCode();
    result = 31 * result + (keyType != null ? keyType.hashCode() : 0);
    result = 31 * result + (valueType != null ? valueType.hashCode() : 0);
    result = 31 * result + (structMetadata != null ? structMetadata.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ThriftType");
    sb.append("{");
    sb.append(protocolType);
    if (structMetadata != null) {
      sb.append(" ").append(structMetadata.getStructClass().getName());
    } else if (keyType != null) {
      sb.append(" keyType=").append(keyType);
      sb.append(", valueType=").append(valueType);
    } else if (valueType != null) {
      sb.append(" valueType=").append(valueType);
    }
    sb.append('}');
    return sb.toString();
  }
}
