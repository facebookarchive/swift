/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift;

import java.util.Set;

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

  @ThriftField(id = 11)
  public Set<Boolean> aBooleanSet;
  @ThriftField(id = 12)
  public Set<Byte> aByteSet;
  @ThriftField(id = 13)
  public Set<Short> aShortSet;
  @ThriftField(id = 14)
  public Set<Integer> aIntegerSet;
  @ThriftField(id = 15)
  public Set<Long> aLongSet;
  @ThriftField(id = 16)
  public Set<Double> aDoubleSet;
  @ThriftField(id = 17)
  public Set<String> aStringSet;
  @ThriftField(id = 18)
  public Set<BonkField> aStructSet;


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
    if (aBooleanSet != null ? !aBooleanSet.equals(that.aBooleanSet) : that.aBooleanSet != null) {
      return false;
    }
    if (aByteSet != null ? !aByteSet.equals(that.aByteSet) : that.aByteSet != null) {
      return false;
    }
    if (aDoubleSet != null ? !aDoubleSet.equals(that.aDoubleSet) : that.aDoubleSet != null) {
      return false;
    }
    if (aIntegerSet != null ? !aIntegerSet.equals(that.aIntegerSet) : that.aIntegerSet != null) {
      return false;
    }
    if (aLongSet != null ? !aLongSet.equals(that.aLongSet) : that.aLongSet != null) {
      return false;
    }
    if (aShortSet != null ? !aShortSet.equals(that.aShortSet) : that.aShortSet != null) {
      return false;
    }
    if (aString != null ? !aString.equals(that.aString) : that.aString != null) {
      return false;
    }
    if (aStringSet != null ? !aStringSet.equals(that.aStringSet) : that.aStringSet != null) {
      return false;
    }
    if (aStruct != null ? !aStruct.equals(that.aStruct) : that.aStruct != null) {
      return false;
    }
    if (aStructSet != null ? !aStructSet.equals(that.aStructSet) : that.aStructSet != null) {
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
    result = 31 * result + (aBooleanSet != null ? aBooleanSet.hashCode() : 0);
    result = 31 * result + (aByteSet != null ? aByteSet.hashCode() : 0);
    result = 31 * result + (aShortSet != null ? aShortSet.hashCode() : 0);
    result = 31 * result + (aIntegerSet != null ? aIntegerSet.hashCode() : 0);
    result = 31 * result + (aLongSet != null ? aLongSet.hashCode() : 0);
    result = 31 * result + (aDoubleSet != null ? aDoubleSet.hashCode() : 0);
    result = 31 * result + (aStringSet != null ? aStringSet.hashCode() : 0);
    result = 31 * result + (aStructSet != null ? aStructSet.hashCode() : 0);
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
    sb.append(", aBooleanSet=").append(aBooleanSet);
    sb.append(", aByteSet=").append(aByteSet);
    sb.append(", aShortSet=").append(aShortSet);
    sb.append(", aIntegerSet=").append(aIntegerSet);
    sb.append(", aLongSet=").append(aLongSet);
    sb.append(", aDoubleSet=").append(aDoubleSet);
    sb.append(", aStringSet=").append(aStringSet);
    sb.append(", aStructSet=").append(aStructSet);
    sb.append('}');
    return sb.toString();
  }
}
