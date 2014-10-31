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

import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftType;
import org.apache.thrift.protocol.TProtocol;

import java.nio.ByteBuffer;

public class ArrayFieldThriftCodec
        implements ThriftCodec<ArrayField>
{
    private final ThriftType type;

    public ArrayFieldThriftCodec(ThriftType type)
    {
        this.type = type;
    }

    @Override
    public ThriftType getType()
    {
        return type;
    }

    @Override
    public ArrayField read(TProtocol protocol)
            throws Exception
    {
        TProtocolReader reader = new TProtocolReader(protocol);

        boolean[] booleanArray = null;
        short[] shortArray = null;
        int[] intArray = null;
        long[] longArray = null;
        double[] doubleArray = null;
        byte[] byteArray = null;

        reader.readStructBegin();

        while (reader.nextField()) {
            switch (reader.getFieldId()) {
                case 1:
                    booleanArray = reader.readBoolArrayField();
                    break;
                case 2:
                    shortArray = reader.readI16ArrayField();
                    break;
                case 3:
                    intArray = reader.readI32ArrayField();
                    break;
                case 4:
                    longArray = reader.readI64ArrayField();
                    break;
                case 5:
                    doubleArray = reader.readDoubleArrayField();
                    break;
                case 6:
                    byteArray = reader.readBinaryField().array();
                    break;
                default:
                    reader.skipFieldData();
            }
        }
        reader.readStructEnd();

        ArrayField arrayField = new ArrayField();
        if (booleanArray != null) {
            arrayField.booleanArray = booleanArray;
        }
        if (shortArray != null) {
            arrayField.shortArray = shortArray;
        }
        if (intArray != null) {
            arrayField.intArray = intArray;
        }
        if (longArray != null) {
            arrayField.longArray = longArray;
        }
        if (doubleArray != null) {
            arrayField.doubleArray = doubleArray;
        }
        if (booleanArray != null) {
            arrayField.byteArray = byteArray;
        }

        return arrayField;
    }

    @Override
    public void write(ArrayField value, TProtocol protocol)
            throws Exception
    {
        TProtocolWriter writer = new TProtocolWriter(protocol);

        writer.writeStructBegin("array");

        boolean[] booleanArray = value.booleanArray;
        if (booleanArray != null) {
            writer.writeBoolArrayField("booleanArray", (short) 1, booleanArray);
        }
        short[] shortArray = value.shortArray;
        if (shortArray != null) {
            writer.writeI16ArrayField("shortArray", (short) 2, shortArray);
        }
        int[] intArray = value.intArray;
        if (intArray != null) {
            writer.writeI32ArrayField("intArray", (short) 3, intArray);
        }
        long[] longArray = value.longArray;
        if (longArray != null) {
            writer.writeI64ArrayField("longArray", (short) 4, longArray);
        }
        double[] doubleArray = value.doubleArray;
        if (doubleArray != null) {
            writer.writeDoubleArrayField("doubleArray", (short) 5, doubleArray);
        }
        byte[] byteArray = value.byteArray;
        if (byteArray != null) {
            writer.writeBinaryField("byteArray", (short) 6, ByteBuffer.wrap(byteArray));
        }

        writer.writeStructEnd();
    }
}
