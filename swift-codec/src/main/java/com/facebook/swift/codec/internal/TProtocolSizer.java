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
package com.facebook.swift.codec.internal;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftProtocolType;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TProtocolSizer
{
    int serializedSizeStructBegin(String name);
    int serializedSizeField(String name, ThriftProtocolType type, short id);
    int serializedSizeStop();

    int serializedSizeBool(boolean value);
    int serializedSizeByte(byte value);
    int serializedSizeI16(short value);
    int serializedSizeI32(int value);
    int serializedSizeI64(long value);
    int serializedSizeDouble(double value);
    int serializedSizeString(String string);
    int serializedSizeBinary(ByteBuffer binary);
    <T extends Enum<T>> int serializedSizeEnum(ThriftCodec<T> enumCodec, T value);

    <T> int serializedSizeStruct(ThriftCodec<T> structCodec, T struct);

    <T> int serializedSizeList(ThriftCodec<List<T>> listCodec, List<T> list);
    <T> int serializedSizeSet(ThriftCodec<Set<T>> setCodec, Set<T> set);
    <K, V> int serializedSizeMap(ThriftCodec<Map<K, V>> mapCodec, Map<K, V> map);

    <T> int serializedSizeListElementCodec(ThriftCodec<T> elementCodec, List<T> list);
    <K, V> int serializedSizeMapElementCodec(ThriftCodec<K> keyCodec, ThriftCodec<V> valueCodec, Map<K,V> map);
    <T> int serializedSizeSetElementCodec(ThriftCodec<T> elementCodec, Set<T> set);

    int serializedSizeBoolArray(boolean[] value);
    int serializedSizeI16Array(short[] value);
    int serializedSizeI32Array(int[] value);
    int serializedSizeI64Array(long[] value);
    int serializedSizeDoubleArray(double[] value);
}
