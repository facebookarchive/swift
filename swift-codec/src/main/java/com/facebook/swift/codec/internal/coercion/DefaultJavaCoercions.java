/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.codec.internal.coercion;

import javax.annotation.concurrent.Immutable;
import java.nio.ByteBuffer;

@Immutable
public final class DefaultJavaCoercions
{
    private DefaultJavaCoercions()
    {
    }

    @FromThrift
    public static Boolean booleanToBoxedBoolean(boolean value)
    {
        return value;
    }

    @ToThrift
    public static boolean boxedBooleanToBoolean(Boolean value)
    {
        return value;
    }

    @FromThrift
    public static Byte byteToBoxedByte(byte value)
    {
        return value;
    }

    @ToThrift
    public static byte boxedByteToByte(Byte value)
    {
        return value;
    }

    @FromThrift
    public static Short shortToBoxedShort(short value)
    {
        return value;
    }

    @ToThrift
    public static short boxedShortToShort(Short value)
    {
        return value;
    }

    @FromThrift
    public static Integer integerToBoxedInteger(int value)
    {
        return value;
    }

    @ToThrift
    public static int boxedIntegerToInteger(Integer value)
    {
        return value;
    }

    @FromThrift
    public static Long longToBoxedLong(long value)
    {
        return value;
    }

    @ToThrift
    public static long boxedLongToLong(Long value)
    {
        return value;
    }

    @FromThrift
    public static float doubleToPrimitiveFloat(double value)
    {
        return (float) value;
    }

    @ToThrift
    public static double primitiveFloatToDouble(float value)
    {
        return value;
    }

    @FromThrift
    public static Float doubleToBoxedFloat(double value)
    {
        return (float) value;
    }

    @ToThrift
    public static double boxedFloatToDouble(Float value)
    {
        return value;
    }

    @FromThrift
    public static Double doubleToBoxedDouble(double value)
    {
        return value;
    }

    @ToThrift
    public static double boxedDoubleToDouble(Double value)
    {
        return value;
    }

    @FromThrift
    public static byte[] byteBufferToByteArray(ByteBuffer buffer)
    {
        byte[] result = new byte[buffer.remaining()];
        buffer.duplicate().get(result);
        return result;
    }

    @ToThrift
    public static ByteBuffer byteArrayToByteBuffer(byte[] value)
    {
        return ByteBuffer.wrap(value);
    }
}
