/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TSet;
import org.apache.thrift.protocol.TType;

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

public class TProtocolReader {
  private final TProtocol protocol;
  private TField currentField;

  public TProtocolReader(TProtocol protocol) {
    this.protocol = protocol;
  }

  public void readStructBegin() throws TException {
    protocol.readStructBegin();
    currentField = null;
  }

  public void readStructEnd() throws TException {
    if (currentField == null || currentField.id != TType.STOP) {
      throw new IllegalStateException("Some fields have not been consumed");
    }

    currentField = null;
    protocol.readStructEnd();
  }

  public boolean nextField() throws TException {
    // if the current field is a stop record, the caller must call readStructEnd.
    if (currentField != null && currentField.id == TType.STOP) {
      throw new NoSuchElementException();
    }
    checkState(currentField == null, "Current field was not read");

    // advance to the next field
    currentField = protocol.readFieldBegin();

    return currentField.type != TType.STOP;
  }

  public short getFieldId() {
    checkState(currentField != null, "No current field");
    return currentField.id;
  }

  public byte getFieldType() {
    checkState(currentField != null, "No current field");
    return currentField.type;
  }

  public void skipFieldData() throws TException {
    TProtocolUtil.skip(protocol, currentField.type);
    protocol.readFieldEnd();
    currentField = null;
  }

  public ByteBuffer readBinary() throws TException {
    if (!checkReadState(TType.STRING)) {
      return null;
    }

    ByteBuffer value = protocol.readBinary();
    currentField = null;
    return value;
  }

  public boolean readBool() throws TException {
    if (!checkReadState(TType.BOOL)) {
      return false;
    }
    currentField = null;
    return protocol.readBool();
  }

  public byte readByte() throws TException {
    if (!checkReadState(TType.BYTE)) {
      return 0;
    }
    currentField = null;
    return protocol.readByte();
  }

  public double readDouble() throws TException {
    if (!checkReadState(TType.DOUBLE)) {
      return 0;
    }
    currentField = null;
    return protocol.readDouble();
  }

  public short readI16() throws TException {
    if (!checkReadState(TType.I16)) {
      return 0;
    }
    currentField = null;
    return protocol.readI16();
  }

  public int readI32() throws TException {
    if (!checkReadState(TType.I32)) {
      return 0;
    }
    currentField = null;
    return protocol.readI32();
  }

  public long readI64() throws TException {
    if (!checkReadState(TType.I64)) {
      return 0;
    }
    currentField = null;
    return protocol.readI64();
  }

  public String readString() throws TException {
    if (!checkReadState(TType.STRING)) {
      return null;
    }
    currentField = null;
    return protocol.readString();
  }

  public <T> T readStruct(ThriftTypeCodec<T> codec) throws Exception {
    if (!checkReadState(TType.STRUCT)) {
      return null;
    }
    currentField = null;
    return codec.read(this);
  }
  public TSet readSetBegin() throws TException {
    return protocol.readSetBegin();
  }

  public void readSetEnd() throws TException {
    currentField = null;
    protocol.readSetEnd();
  }

  public <T> Set<T> readSet(ThriftTypeCodec<T> valueType) throws Exception {
    if (!checkReadState(TType.SET)) {
      return null;
    }
    currentField = null;

    TSet tSet = readSetBegin();
    ImmutableSet.Builder<T> set = ImmutableSet.builder();
    for (int i = 0; i < tSet.size; i++) {
      T element = valueType.read(this);
      set.add(element);
    }
    protocol.readSetEnd();
    return set.build();
  }

  public TList readListBegin() throws TException {
    return protocol.readListBegin();
  }

  public void readListEnd() throws TException {
    currentField = null;
    protocol.readListEnd();
  }

  public TMap readMapBegin() throws TException {
    return protocol.readMapBegin();
  }

  public void readMapEnd() throws TException {
    currentField = null;
    protocol.readMapEnd();
  }

  private boolean checkReadState(byte expectedType) throws TException {
    checkState(currentField != null, "No current field");

    if (currentField.type != expectedType) {
      TProtocolUtil.skip(protocol, currentField.type);
      protocol.readFieldEnd();
      currentField = null;
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("TProtocolReader");
    sb.append("{currentField=").append(currentField);
    sb.append('}');
    return sb.toString();
  }
}
