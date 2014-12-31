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
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TProtocolSizers
{
    public static class TCompactProtocolSizer implements TProtocolSizer
    {
        @Override public int serializedSizeStructBegin(String name) { return 0; }
        @Override public int serializedSizeField(String name, ThriftProtocolType type, short id) { return 4; /* byte + i16 */}
        @Override public int serializedSizeStop() { return 1; }

        @Override public int serializedSizeBool(boolean value) { return 1; }
        @Override public int serializedSizeByte(byte value) { return 1; }
        @Override public int serializedSizeI16(short value) { return 3; }
        @Override public int serializedSizeI32(int value) { return 5; }
        @Override public int serializedSizeI64(long value) { return 10; }
        @Override public int serializedSizeDouble(double value) { return 8; }

        // The maximum inflation factor when converting UTF-16 (characters) to UTF-8 (bytes) is 3:
        // Codepoints in the range 0x0800-0xFFFF take 1 UTF-16 character or 3 UTF-8 bytes.
        // Note that surrogate pairs (codepoints >0xFFFF) take 2 UTF-16 characters or 4 UTF-8 bytes
        // so the inflation factor for them is only 2.
        @Override public int serializedSizeString(String value) { return 5 + value.length() * 3; }
        @Override public int serializedSizeBinary(ByteBuffer value) { return 5 + value.limit() - value.position(); }

        @Override public <T extends Enum<T>> int serializedSizeEnum(ThriftCodec<T> enumCodec, T value)
        {
            return enumCodec.serializedSize(value, this);
        }

        @Override public <T> int serializedSizeStruct(ThriftCodec<T> structCodec, T struct)
        {
            return structCodec.serializedSize(struct, this);
        }

        @Override public <T> int serializedSizeList(ThriftCodec<List<T>> listCodec, List<T> list)
        {
            return listCodec.serializedSize(list, this);
        }

        @Override public <K, V> int serializedSizeMap(ThriftCodec<Map<K, V>> mapCodec, Map<K, V> map)
        {
            return mapCodec.serializedSize(map, this);
        }

        @Override public <T> int serializedSizeSet(ThriftCodec<Set<T>> setCodec, Set<T> set)
        {
            return setCodec.serializedSize(set, this);
        }

        @Override public <T> int serializedSizeListElementCodec(ThriftCodec<T> elementCodec, List<T> list)
        {
            int size = 6;   // serializedSizeByte + serializedSizeI32
            for (T elem : list) {
                size += elementCodec.serializedSize(elem, this);
            }
            return size;
        }

        @Override public <K, V> int serializedSizeMapElementCodec(ThriftCodec<K> keyCodec, ThriftCodec<V> valueCodec, Map<K,V> map)
        {
            int size = 7;   // serializedSizeByte*2 + serializedSizeI32
            for (Map.Entry<K, V> entry : map.entrySet()) {
                size += keyCodec.serializedSize(entry.getKey(), this);
                size += valueCodec.serializedSize(entry.getValue(), this);
            }
            return size;
        }

        @Override public <T> int serializedSizeSetElementCodec(ThriftCodec<T> elementCodec, Set<T> set)
        {
            int size = 6;   // serializedSizeByte + serializedSizeI32
            for (T elem : set) {
                size += elementCodec.serializedSize(elem, this);
            }
            return size;
        }

        @Override public int serializedSizeBoolArray(boolean[] value)
        {
            return 6 + value.length;
        }

        @Override public int serializedSizeI16Array(short[] value)
        {
            return 6 + 3 * value.length;
        }

        @Override public int serializedSizeI32Array(int[] value)
        {
            return 6 + 5 * value.length;
        }

        @Override public int serializedSizeI64Array(long[] value)
        {
            return 6 + 10 * value.length;
        }

        @Override public int serializedSizeDoubleArray(double[] value)
        {
            return 6 + 8 * value.length;
        }
    }

    public static final TProtocolSizer COMPACT_SIZER = new TCompactProtocolSizer();

    public static TProtocolSizer fromProtocol(TProtocol protocol)
    {
        if (protocol.getClass() == TCompactProtocol.class) {
            return COMPACT_SIZER;
        } else {
            throw new IllegalArgumentException("Unknown protocol");
        }
    }
}
