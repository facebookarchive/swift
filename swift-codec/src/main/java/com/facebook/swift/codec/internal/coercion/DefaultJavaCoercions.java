/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal.coercion;

import java.nio.ByteBuffer;

import static com.google.common.base.Charsets.UTF_8;

public final class DefaultJavaCoercions {
  private DefaultJavaCoercions() {
  }

  @FromThrift
  public static Boolean booleanToBoxedBoolean(boolean value) {
    return value;
  }

  @ToThrift
  public static boolean boxedBooleanToBoolean(Boolean value) {
    return value;
  }

  @FromThrift
  public static Byte byteToBoxedByte(byte value) {
    return value;
  }

  @ToThrift
  public static byte boxedByteToByte(Byte value) {
    return value;
  }

  @FromThrift
  public static Short shortToBoxedShort(short value) {
    return value;
  }

  @ToThrift
  public static short boxedShortToShort(Short value) {
    return value;
  }

  @FromThrift
  public static Integer integerToBoxedInteger(int value) {
    return value;
  }

  @ToThrift
  public static int boxedIntegerToInteger(Integer value) {
    return value;
  }

  @FromThrift
  public static Long longToBoxedLong(long value) {
    return value;
  }

  @ToThrift
  public static long boxedLongToLong(Long value) {
    return value;
  }

  @FromThrift
  public static float doubleToPrimitiveFloat(double value) {
    return (float) value;
  }

  @ToThrift
  public static double primitiveFloatToDouble(float value) {
    return value;
  }

  @FromThrift
  public static Float doubleToBoxedFloat(double value) {
    return (float) value;
  }

  @ToThrift
  public static double boxedFloatToDouble(Float value) {
    return value;
  }

  @FromThrift
  public static Double doubleToBoxedDouble(double value) {
    return value;
  }

  @ToThrift
  public static double boxedDoubleToDouble(Double value) {
    return value;
  }

  @FromThrift
  public static String byteBufferToString(ByteBuffer value) {
    if (value == null) {
      return null;
    }
    return new String(value.array(), value.arrayOffset(), value.remaining(), UTF_8);
  }

  @ToThrift
  public static ByteBuffer stringToByteBuffer(String value) {
    if (value == null) {
      return null;
    }
    return ByteBuffer.wrap(value.getBytes(UTF_8));
  }
}
