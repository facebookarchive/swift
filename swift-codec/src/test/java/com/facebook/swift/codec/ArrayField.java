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
package com.facebook.swift.codec;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ThriftStruct("Array")
public final class ArrayField
{
    @ThriftField(1)
    public boolean[] booleanArray;

    @ThriftField(2)
    public short[] shortArray;

    @ThriftField(3)
    public int[] intArray;

    @ThriftField(4)
    public long[] longArray;

    @ThriftField(5)
    public double[] doubleArray;

    @ThriftField(6)
    public byte[] byteArray;

    @ThriftField(11)
    public Map<Short, boolean[]> mapBooleanArray;

    @ThriftField(12)
    public Map<Short, short[]> mapShortArray;

    @ThriftField(13)
    public Map<Short, int[]> mapIntArray;

    @ThriftField(14)
    public Map<Short, long[]> mapLongArray;

    @ThriftField(15)
    public Map<Short, double[]> mapDoubleArray;

    public ArrayField()
    {
    }

    public ArrayField(boolean[] booleanArray, short[] shortArray, int[] intArray, long[] longArray, double[] doubleArray, byte[] byteArray)
    {
        this.booleanArray = booleanArray;
        this.shortArray = shortArray;
        this.intArray = intArray;
        this.longArray = longArray;
        this.doubleArray = doubleArray;
        this.byteArray = byteArray;
    }

    public ArrayField(boolean[] booleanArray,
            short[] shortArray,
            int[] intArray,
            long[] longArray,
            double[] doubleArray,
            byte[] byteArray,
            Map<Short, boolean[]> mapBooleanArray,
            Map<Short, short[]> mapShortArray,
            Map<Short, int[]> mapIntArray,
            Map<Short, long[]> mapLongArray,
            Map<Short, double[]> mapDoubleArray)
    {
        this.booleanArray = booleanArray;
        this.shortArray = shortArray;
        this.intArray = intArray;
        this.longArray = longArray;
        this.doubleArray = doubleArray;
        this.byteArray = byteArray;
        this.mapBooleanArray = mapBooleanArray;
        this.mapShortArray = mapShortArray;
        this.mapIntArray = mapIntArray;
        this.mapLongArray = mapLongArray;
        this.mapDoubleArray = mapDoubleArray;
    }

    public Map<Short, List<Boolean>> getMapBooleanList()
    {
        if (mapBooleanArray == null) {
            return null;
        }
        return Maps.transformValues(mapBooleanArray, booleanArrayAsList());
    }

    public Map<Short, List<Short>> getMapShortList()
    {
        if (mapShortArray == null) {
            return null;
        }
        return Maps.transformValues(mapShortArray, shortArrayAsList());
    }

    public Map<Short, List<Integer>> getMapIntegerList()
    {
        if (mapIntArray == null) {
            return null;
        }
        return Maps.transformValues(mapIntArray, intArrayAsList());
    }

    public Map<Short, List<Long>> getMapLongList()
    {
        if (mapLongArray == null) {
            return null;
        }
        return Maps.transformValues(this.mapLongArray, longArrayAsList());
    }

    public Map<Short, List<Double>> getMapDoubleList()
    {
        if (mapDoubleArray == null) {
            return null;
        }
        return Maps.transformValues(mapDoubleArray, doubleArrayAsList());
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(
                booleanArray,
                shortArray,
                intArray,
                longArray,
                doubleArray,
                byteArray,
                getMapBooleanList(),
                getMapShortList(),
                getMapIntegerList(),
                getMapLongList(),
                getMapDoubleList());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ArrayField other = (ArrayField) obj;
        return Arrays.equals(this.booleanArray, other.booleanArray) &&
                Arrays.equals(this.shortArray, other.shortArray) &&
                Arrays.equals(this.intArray, other.intArray) &&
                Arrays.equals(this.longArray, other.longArray) &&
                Arrays.equals(this.doubleArray, other.doubleArray) &&
                Arrays.equals(this.byteArray, other.byteArray) &&
                Objects.equal(getMapBooleanList(), other.getMapBooleanList()) &&
                Objects.equal(getMapShortList(), other.getMapShortList()) &&
                Objects.equal(getMapIntegerList(), other.getMapIntegerList()) &&
                Objects.equal(getMapLongList(), other.getMapLongList()) &&
                Objects.equal(getMapDoubleList(), other.getMapDoubleList());
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("booleanArray", Arrays.toString(booleanArray))
                          .add("shortArray", Arrays.toString(shortArray))
                          .add("intArray", Arrays.toString(intArray))
                          .add("longArray", Arrays.toString(longArray))
                          .add("doubleArray", Arrays.toString(doubleArray))
                          .add("byteArray", Arrays.toString(byteArray))
                          .add("mapBooleanArray", getMapBooleanList())
                          .add("mapShortArray", getMapShortList())
                          .add("mapIntArray", getMapIntegerList())
                          .add("mapLongArray", getMapLongList())
                          .add("mapDoubleArray", getMapDoubleList())
                          .toString();
    }

    private static Function<boolean[], List<Boolean>> booleanArrayAsList()
    {
        return new Function<boolean[], List<Boolean>>()
        {
            @Nullable
            @Override
            public List<Boolean> apply(@Nullable boolean[] input)
            {
                if (input == null) {
                    return null;
                }
                return Booleans.asList(input);
            }
        };
    }

    private static Function<short[], List<Short>> shortArrayAsList()
    {
        return new Function<short[], List<Short>>()
        {
            @Nullable
            @Override
            public List<Short> apply(@Nullable short[] input)
            {
                if (input == null) {
                    return null;
                }
                return Shorts.asList(input);
            }
        };
    }

    private static Function<int[], List<Integer>> intArrayAsList()
    {
        return new Function<int[], List<Integer>>()
        {
            @Nullable
            @Override
            public List<Integer> apply(@Nullable int[] input)
            {
                if (input == null) {
                    return null;
                }
                return Ints.asList(input);
            }
        };
    }

    private static Function<long[], List<Long>> longArrayAsList()
    {
        return new Function<long[], List<Long>>()
        {
            @Nullable
            @Override
            public List<Long> apply(@Nullable long[] input)
            {
                if (input == null) {
                    return null;
                }
                return Longs.asList(input);
            }
        };
    }

    private static Function<double[], List<Double>> doubleArrayAsList()
    {
        return new Function<double[], List<Double>>()
        {
            @Nullable
            @Override
            public List<Double> apply(@Nullable double[] input)
            {
                if (input == null) {
                    return null;
                }
                return Doubles.asList(input);
            }
        };
    }
}
