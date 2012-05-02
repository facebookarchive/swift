/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TSet;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TProtocolWriter {
  private final TProtocol protocol;

  public TProtocolWriter(TProtocol protocol) {
    this.protocol = protocol;
  }

  public void writeStructBegin(String name) throws TException {
    protocol.writeStructBegin(new TStruct(name));
  }

  public void writeStructEnd() throws TException {
    protocol.writeFieldStop();
    protocol.writeStructEnd();
  }

  public void writeBinaryField(String name, short id, ByteBuffer buf) throws TException {
    protocol.writeFieldBegin(new TField(name, TType.STRING, id));
    protocol.writeBinary(buf);
    protocol.writeFieldEnd();
  }

  public void writeBoolField(String name, short id, boolean b) throws TException {
    protocol.writeFieldBegin(new TField(name, TType.BOOL, id));
    protocol.writeBool(b);
    protocol.writeFieldEnd();
  }

  public void writeByteField(String name, short id, byte b) throws TException {
    protocol.writeFieldBegin(new TField(name, TType.BYTE, id));
    protocol.writeByte(b);
    protocol.writeFieldEnd();
  }

  public void writeDoubleField(String name, short id, double dub) throws TException {
    protocol.writeFieldBegin(new TField(name, TType.DOUBLE, id));
    protocol.writeDouble(dub);
    protocol.writeFieldEnd();
  }

  public void writeI16Field(String name, short id, short i16) throws TException {
    protocol.writeFieldBegin(new TField(name, TType.I16, id));
    protocol.writeI16(i16);
    protocol.writeFieldEnd();
  }

  public void writeI32Field(String name, short id, int i32) throws TException {
    protocol.writeFieldBegin(new TField(name, TType.I32, id));
    protocol.writeI32(i32);
    protocol.writeFieldEnd();
  }

  public void writeI64Field(String name, short id, long i64) throws TException {
    protocol.writeFieldBegin(new TField(name, TType.I64, id));
    protocol.writeI64(i64);
    protocol.writeFieldEnd();
  }

  public void writeStringField(String name, short id, String string) throws TException {
    if (string == null) {
      return;
    }

    protocol.writeFieldBegin(new TField(name, TType.STRING, id));
    protocol.writeString(string);
    protocol.writeFieldEnd();
  }

  public <T> void writeStructField(String name, short id, ThriftTypeCodec<T> codec, T struct)
      throws Exception {
    if (struct == null) {
      return;
    }

    protocol.writeFieldBegin(new TField(name, TType.STRUCT, id));
    codec.write(struct, this);
    protocol.writeFieldEnd();
  }

  public <E> void writeSetField(String name, short id, ThriftTypeCodec<Set<E>> codec, Set<E> set)
      throws Exception {
    if (set == null) {
      return;
    }

    protocol.writeFieldBegin(new TField(name, TType.SET, id));
    codec.write(set, this);
    protocol.writeFieldEnd();
  }

  public <E> void writeListField(String name, short id, ThriftTypeCodec<List<E>> codec, List<E> list)
      throws Exception {
    if (list == null) {
      return;
    }

    protocol.writeFieldBegin(new TField(name, TType.LIST, id));
    codec.write(list, this);
    protocol.writeFieldEnd();
  }

  public <K,V> void writeMapField(String name, short id, ThriftTypeCodec<Map<K,V>> codec, Map<K,V> map)
      throws Exception {
    if (map == null) {
      return;
    }

    protocol.writeFieldBegin(new TField(name, TType.MAP, id));
    codec.write(map, this);
    protocol.writeFieldEnd();
  }

  public void writeBinary(ByteBuffer buf) throws TException {
    protocol.writeBinary(buf);
  }

  public void writeBool(boolean b) throws TException {
    protocol.writeBool(b);
  }

  public void writeByte(byte b) throws TException {
    protocol.writeByte(b);
  }

  public void writeI16(short i16) throws TException {
    protocol.writeI16(i16);
  }

  public void writeI32(int i32) throws TException {
    protocol.writeI32(i32);
  }

  public void writeI64(long i64) throws TException {
    protocol.writeI64(i64);
  }

  public void writeDouble(double dub) throws TException {
    protocol.writeDouble(dub);
  }

  public void writeString(String str) throws TException {
    protocol.writeString(str);
  }

  public <T> void writeSet(ThriftTypeCodec<T> elementCodec, Set<T> set) throws Exception {
    if (set == null) {
      return;
    }

    protocol.writeSetBegin(
        new TSet(
            elementCodec.getType().getProtocolType().getType(),
            set.size()
        )
    );

    for (T element : set) {
      elementCodec.write(element, this);
    }

    protocol.writeSetEnd();
  }

  public <T> void writeList(ThriftTypeCodec<T> elementCodec, List<T> list) throws Exception {
    if (list == null) {
      return;
    }

    protocol.writeListBegin(
        new TList(
            elementCodec.getType().getProtocolType().getType(),
            list.size()
        )
    );

    for (T element : list) {
      elementCodec.write(element, this);
    }

    protocol.writeListEnd();
  }

  public <K, V> void writeMap(
      ThriftTypeCodec<K> keyCodec,
      ThriftTypeCodec<V> valueCodec,
      Map<K, V> map
  ) throws Exception {

    if (map == null) {
      return;
    }

    protocol.writeMapBegin(
        new TMap(
            keyCodec.getType().getProtocolType().getType(),
            valueCodec.getType().getProtocolType().getType(),
            map.size()
        )
    );

    for (Map.Entry<K, V> entry : map.entrySet()) {
      keyCodec.write(entry.getKey(), this);
      valueCodec.write(entry.getValue(), this);
    }

    protocol.writeMapEnd();
  }
}
