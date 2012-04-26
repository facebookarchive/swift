/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.compiler;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TSet;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;

import java.nio.ByteBuffer;

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

    public void writeBinary(String name, short id, ByteBuffer buf)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.STRING, id));
        protocol.writeBinary(buf);
        protocol.writeFieldEnd();
    }

    public void writeBool(String name, short id, boolean b)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.BOOL, id));
        protocol.writeBool(b);
        protocol.writeFieldEnd();
    }

    public void writeByte(String name, short id, byte b)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.BYTE, id));
        protocol.writeByte(b);
        protocol.writeFieldEnd();
    }

    public void writeDouble(String name, short id, double dub)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.DOUBLE, id));
        protocol.writeDouble(dub);
        protocol.writeFieldEnd();
    }

    public void writeI16(String name, short id, short i16)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.I16, id));
        protocol.writeI16(i16);
        protocol.writeFieldEnd();
    }

    public void writeI32(String name, short id, int i32)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.I32, id));
        protocol.writeI32(i32);
        protocol.writeFieldEnd();
    }

    public void writeI64(String name, short id, long i64)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.I64, id));
        protocol.writeI64(i64);
        protocol.writeFieldEnd();
    }

    public void writeString(String name, short id, String string)
            throws TException
    {
        protocol.writeFieldBegin(new TField(name, TType.STRING, id));
        protocol.writeString(string);
        protocol.writeFieldEnd();
    }

    public void writeListBegin(TList list)
            throws TException
    {
        protocol.writeListBegin(list);
    }

    public void writeListEnd()
            throws TException
    {
        protocol.writeListEnd();
    }

    public void writeMapBegin(TMap map)
            throws TException
    {
        protocol.writeMapBegin(map);
    }

    public void writeMapEnd()
            throws TException
    {
        protocol.writeMapEnd();
    }

    public void writeSetBegin(TSet set)
            throws TException
    {
        protocol.writeSetBegin(set);
    }

    public void writeSetEnd()
            throws TException
    {
        protocol.writeSetEnd();
    }

}
