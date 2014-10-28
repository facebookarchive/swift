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
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TSet;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

@NotThreadSafe
public class TProtocolWriter
{
    private final TProtocol protocol;

    public TProtocolWriter(TProtocol protocol)
    {
        this.protocol = protocol;
    }

    public void writeStructBegin(String name)
            throws TException
    {
        protocol.writeStructBegin(new TStruct(name));
    }

    public void writeStructEnd()
            throws TException
    {
        protocol.writeFieldStop();
        protocol.writeStructEnd();
    }

    public <T> void writeField(String name, short id, ThriftCodec<T> codec, T value)
            throws Exception
    {
        if (value == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, codec.getType().getProtocolType().getType(), id));
        codec.write(value, protocol);
        protocol.writeFieldEnd();
    }

    public void writeBinaryField(String name, short id, ByteBuffer buf)
            throws TException
    {
        if (buf == null) {
            return;
        }
        protocol.writeFieldBegin(new TField(name, TType.STRING, id));
        protocol.writeBinary(buf);
        protocol.writeFieldEnd();
    }

    public void writeBoolField(String name, short id, boolean b)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.BOOL, id));
        protocol.writeBool(b);
        protocol.writeFieldEnd();
    }

    public void writeByteField(String name, short id, byte b)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.BYTE, id));
        protocol.writeByte(b);
        protocol.writeFieldEnd();
    }

    public void writeDoubleField(String name, short id, double dub)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.DOUBLE, id));
        protocol.writeDouble(dub);
        protocol.writeFieldEnd();
    }

    public void writeI16Field(String name, short id, short i16)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.I16, id));
        protocol.writeI16(i16);
        protocol.writeFieldEnd();
    }

    public void writeI32Field(String name, short id, int i32)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.I32, id));
        protocol.writeI32(i32);
        protocol.writeFieldEnd();
    }

    public void writeI64Field(String name, short id, long i64)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.I64, id));
        protocol.writeI64(i64);
        protocol.writeFieldEnd();
    }

    public void writeStringField(String name, short id, String string)
            throws TException
    {
        if (string == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.STRING, id));
        protocol.writeString(string);
        protocol.writeFieldEnd();
    }

    public <T> void writeStructField(String name, short id, ThriftCodec<T> codec, T struct)
            throws Exception
    {
        if (struct == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.STRUCT, id));
        codec.write(struct, protocol);
        protocol.writeFieldEnd();
    }

    public void writeBoolArrayField(String name, short id, boolean[] array)
            throws Exception
    {
        if (array == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.LIST, id));
        writeBoolArray(array);
        protocol.writeFieldEnd();
    }

    public void writeI16ArrayField(String name, short id, short[] array)
            throws Exception
    {
        if (array == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.LIST, id));
        writeI16Array(array);
        protocol.writeFieldEnd();
    }

    public void writeI32ArrayField(String name, short id, int[] array)
            throws Exception
    {
        if (array == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.LIST, id));
        writeI32Array(array);
        protocol.writeFieldEnd();
    }

    public void writeI64ArrayField(String name, short id, long[] array)
            throws Exception
    {
        if (array == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.LIST, id));
        writeI64Array(array);
        protocol.writeFieldEnd();
    }

    public void writeDoubleArrayField(String name, short id, double[] array)
            throws Exception
    {
        if (array == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.LIST, id));
        writeDoubleArray(array);
        protocol.writeFieldEnd();
    }

    public <E> void writeSetField(String name, short id, ThriftCodec<Set<E>> codec, Set<E> set)
            throws Exception
    {
        if (set == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.SET, id));
        codec.write(set, protocol);
        protocol.writeFieldEnd();
    }

    public <E> void writeListField(String name, short id, ThriftCodec<List<E>> codec, List<E> list)
            throws Exception
    {
        if (list == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.LIST, id));
        codec.write(list, protocol);
        protocol.writeFieldEnd();
    }

    public <K, V> void writeMapField(String name, short id, ThriftCodec<Map<K, V>> codec, Map<K, V> map)
            throws Exception
    {
        if (map == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.MAP, id));
        codec.write(map, protocol);
        protocol.writeFieldEnd();
    }

    public <T extends Enum<T>> void writeEnumField(String name, short id, ThriftCodec<T> codec, T enumValue)
            throws Exception
    {

        if (enumValue == null) {
            return;
        }

        protocol.writeFieldBegin(new TField(name, TType.I32, id));
        codec.write(enumValue, protocol);
        protocol.writeFieldEnd();
    }

    public void writeBinary(ByteBuffer buf)
            throws TException
    {
        if (buf == null) {
            return;
        }
        protocol.writeBinary(buf);
    }

    public void writeBool(boolean b)
            throws TException
    {
        protocol.writeBool(b);
    }

    public void writeByte(byte b)
            throws TException
    {
        protocol.writeByte(b);
    }

    public void writeI16(short i16)
            throws TException
    {
        protocol.writeI16(i16);
    }

    public void writeI32(int i32)
            throws TException
    {
        protocol.writeI32(i32);
    }

    public void writeI64(long i64)
            throws TException
    {
        protocol.writeI64(i64);
    }

    public void writeDouble(double dub)
            throws TException
    {
        protocol.writeDouble(dub);
    }

    public void writeString(String string)
            throws TException
    {
        if (string == null) {
            return;
        }
        protocol.writeString(string);
    }

    public void writeBoolArray(boolean[] array)
            throws TException
    {
        protocol.writeListBegin(new TList(TType.BOOL, array.length));
        for (boolean booleanValue : array) {
            writeBool(booleanValue);
        }
        protocol.writeListEnd();
    }

    public void writeI16Array(short[] array)
            throws TException
    {
        protocol.writeListBegin(new TList(TType.I16, array.length));
        for (int i16 : array) {
            writeI32(i16);
        }
        protocol.writeListEnd();
    }

    public void writeI32Array(int[] array)
            throws TException
    {
        protocol.writeListBegin(new TList(TType.I32, array.length));
        for (int i32 : array) {
            writeI32(i32);
        }
        protocol.writeListEnd();
    }

    public void writeI64Array(long[] array)
            throws TException
    {
        protocol.writeListBegin(new TList(TType.I64, array.length));
        for (long i64 : array) {
            writeI64(i64);
        }
        protocol.writeListEnd();
    }

    public void writeDoubleArray(double[] array)
            throws TException
    {
        protocol.writeListBegin(new TList(TType.DOUBLE, array.length));
        for (double doubleValue : array) {
            writeDouble(doubleValue);
        }
        protocol.writeListEnd();
    }

    public <T> void writeSet(ThriftCodec<T> elementCodec, Set<T> set)
            throws Exception
    {
        if (set == null) {
            return;
        }

        protocol.writeSetBegin(new TSet(elementCodec.getType().getProtocolType().getType(), set.size()));

        for (T element : set) {
            elementCodec.write(element, protocol);
        }

        protocol.writeSetEnd();
    }

    public <T> void writeList(ThriftCodec<T> elementCodec, List<T> list)
            throws Exception
    {
        if (list == null) {
            return;
        }

        protocol.writeListBegin(new TList(elementCodec.getType().getProtocolType().getType(), list.size()));

        for (T element : list) {
            elementCodec.write(element, protocol);
        }

        protocol.writeListEnd();
    }

    public <K, V> void writeMap(ThriftCodec<K> keyCodec, ThriftCodec<V> valueCodec, Map<K, V> map)
            throws Exception
    {

        if (map == null) {
            return;
        }

        protocol.writeMapBegin(new TMap(keyCodec.getType().getProtocolType().getType(), valueCodec.getType().getProtocolType().getType(), map.size()));

        for (Map.Entry<K, V> entry : map.entrySet()) {
            keyCodec.write(entry.getKey(), protocol);
            valueCodec.write(entry.getValue(), protocol);
        }

        protocol.writeMapEnd();
    }
}
