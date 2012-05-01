/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift;

@ThriftStruct
public class OneOfEverything {
  @ThriftField(id = 1)
  public boolean aBoolean;
  @ThriftField(id = 2)
  public byte aByte;
  @ThriftField(id = 3)
  public short aShort;
  @ThriftField(id = 4)
  public int aInt;
  @ThriftField(id = 5)
  public long aLong;
  @ThriftField(id = 6)
  public double aDouble;
  @ThriftField(id = 7)
  public String aString;
  @ThriftField(id = 8)
  public BonkField aStruct;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final OneOfEverything that = (OneOfEverything) o;

    if (aBoolean != that.aBoolean) {
      return false;
    }
    if (aByte != that.aByte) {
      return false;
    }
    if (Double.compare(that.aDouble, aDouble) != 0) {
      return false;
    }
    if (aInt != that.aInt) {
      return false;
    }
    if (aLong != that.aLong) {
      return false;
    }
    if (aShort != that.aShort) {
      return false;
    }
    if (aString != null ? !aString.equals(that.aString) : that.aString != null) {
      return false;
    }
    if (aStruct != null ? !aStruct.equals(that.aStruct) : that.aStruct != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    result = (aBoolean ? 1 : 0);
    result = 31 * result + (int) aByte;
    result = 31 * result + (int) aShort;
    result = 31 * result + aInt;
    result = 31 * result + (int) (aLong ^ (aLong >>> 32));
    temp = aDouble != +0.0d ? Double.doubleToLongBits(aDouble) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + (aString != null ? aString.hashCode() : 0);
    result = 31 * result + (aStruct != null ? aStruct.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("OneOfEverything");
    sb.append("{aBoolean=").append(aBoolean);
    sb.append(", aByte=").append(aByte);
    sb.append(", aShort=").append(aShort);
    sb.append(", aInt=").append(aInt);
    sb.append(", aLong=").append(aLong);
    sb.append(", aDouble=").append(aDouble);
    sb.append(", aString='").append(aString).append('\'');
    sb.append(", aStruct=").append(aStruct);
    sb.append('}');
    return sb.toString();
  }
}
