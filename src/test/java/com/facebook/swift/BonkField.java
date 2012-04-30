/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift;

@ThriftStruct(name = "Bonk")
public class BonkField {
  @ThriftField(id = 1)
  public String message;

  @ThriftField(id = 2)
  public int type;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BonkField bonkField = (BonkField) o;

    if (type != bonkField.type) {
      return false;
    }
    if (message != null ? !message.equals(bonkField.message) : bonkField.message != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = message != null ? message.hashCode() : 0;
    result = 31 * result + type;
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("BonkField");
    sb.append("{message='").append(message).append('\'');
    sb.append(", type=").append(type);
    sb.append('}');
    return sb.toString();
  }
}
