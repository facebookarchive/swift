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

public class OneOfEverythingThriftCodec implements ThriftCodec<OneOfEverything>
{

    private final ThriftType type;
    private final ThriftCodec<BonkField> aStructCodec;
    private final ThriftCodec<UnionField> aUnionCodec;
    private final ThriftCodec<Fruit> aFruitCodec;

    public OneOfEverythingThriftCodec(ThriftType type,
                                      ThriftCodec<BonkField> aStructCodec,
                                      ThriftCodec<UnionField> aUnionCodec,
                                      ThriftCodec<Fruit> aFruitCodec)
    {
        this.type = type;
        this.aStructCodec = aStructCodec;
        this.aUnionCodec = aUnionCodec;
        this.aFruitCodec = aFruitCodec;
    }

    @Override
    public ThriftType getType()
    {
        return type;
    }

    @Override
    public OneOfEverything read(TProtocol protocol)
            throws Exception
    {
        TProtocolReader reader = new TProtocolReader(protocol);

        boolean aBoolean = false;
        byte aByte = 0;
        short aShort = 0;
        int aInt = 0;
        long aLong = 0;
        double aDouble = 0;
        String aString = null;
        BonkField aStruct = null;
        Fruit aEnum = null;
        UnionField aUnion = null;

        reader.readStructBegin();

        while (reader.nextField()) {
            switch (reader.getFieldId()) {
                case 1:
                    aBoolean = reader.readBoolField();
                    break;
                case 2:
                    aByte = reader.readByteField();
                    break;
                case 3:
                    aShort = reader.readI16Field();
                    break;
                case 4:
                    aInt = reader.readI32Field();
                    break;
                case 5:
                    aLong = reader.readI64Field();
                    break;
                case 6:
                    aDouble = reader.readDoubleField();
                    break;
                case 7:
                    aString = reader.readStringField();
                    break;
                case 8:
                    aStruct = reader.readStructField(aStructCodec);
                    break;
                case 9:
                    aEnum = reader.readEnumField(aFruitCodec);
                    break;
                case 60:
                    aUnion = reader.readStructField(aUnionCodec);
                default:
                    reader.skipFieldData();
            }
        }
        reader.readStructEnd();

        OneOfEverything oneOfEverything = new OneOfEverything();
        oneOfEverything.aBoolean = aBoolean;
        oneOfEverything.aByte = aByte;
        oneOfEverything.aShort = aShort;
        oneOfEverything.aInt = aInt;
        oneOfEverything.aLong = aLong;
        oneOfEverything.aDouble = aDouble;
        oneOfEverything.aString = aString;
        oneOfEverything.aStruct = aStruct;
        oneOfEverything.aEnum = aEnum;
        oneOfEverything.aUnion = aUnion;

        return oneOfEverything;
    }

    @Override
    public void write(OneOfEverything oneOfEverything, TProtocol protocol)
            throws Exception
    {
        TProtocolWriter writer = new TProtocolWriter(protocol);

        writer.writeStructBegin("OneOfEverything");
        writer.writeBoolField("aBoolean", (short) 1, oneOfEverything.aBoolean);
        writer.writeByteField("aByte", (short) 2, oneOfEverything.aByte);
        writer.writeI16Field("aShort", (short) 3, oneOfEverything.aShort);
        writer.writeI32Field("aInt", (short) 4, oneOfEverything.aInt);
        writer.writeI64Field("aLong", (short) 5, oneOfEverything.aLong);
        writer.writeDoubleField("aDouble", (short) 6, oneOfEverything.aDouble);
        writer.writeStringField("aString", (short) 7, oneOfEverything.aString);
        writer.writeStructField("aStruct", (short) 8, aStructCodec, oneOfEverything.aStruct);
        writer.writeEnumField("aEnum", (short) 9, aFruitCodec, oneOfEverything.aEnum);
        writer.writeStructField("aUnion", (short) 61, aUnionCodec, oneOfEverything.aUnion);
        writer.writeStructEnd();
    }
}
