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
import com.google.common.base.Preconditions;

import org.apache.thrift.protocol.TProtocol;

public class UnionFieldThriftCodec implements ThriftCodec<UnionField>
{
    private final ThriftType type;
    private final ThriftCodec<Fruit> fruitCodec;

    public UnionFieldThriftCodec(ThriftType type, ThriftCodec<Fruit> fruitCodec)
    {
        this.type = type;
        this.fruitCodec = fruitCodec;
    }

    @Override
    public ThriftType getType()
    {
        return type;
    }

    @Override
    public UnionField read(TProtocol protocol)
            throws Exception
    {
        TProtocolReader reader = new TProtocolReader(protocol);

        UnionField field = new UnionField();
        reader.readStructBegin();

        boolean consumed = false;
        while(reader.nextField()) {
            Preconditions.checkState(!consumed, "already consumed");

            field._id = reader.getFieldId();
            switch (field._id) {
            case 1:
                field.stringValue = reader.readStringField();
                consumed = true;
                break;
            case 2:
                field.longValue = reader.readI64Field();
                consumed = true;
                break;
            case 3:
                field.fruitValue = reader.readEnumField(fruitCodec);
                consumed = true;
                break;
            default:
                field._id = -1;
                reader.skipFieldData();
            }
        }
        reader.readStructEnd();

        return field;
    }

    @Override
    public void write(UnionField value, TProtocol protocol)
            throws Exception
    {
        TProtocolWriter writer = new TProtocolWriter(protocol);

        writer.writeStructBegin("union");

        switch (value._id) {
        case 1:
            writer.writeStringField("stringValue", (short) 1, value.stringValue);
            break;
        case 2:
            writer.writeI64Field("longValue", (short) 2, value.longValue);
            break;
        case 3:
            writer.writeEnumField("fruitValue", (short) 3, fruitCodec, value.fruitValue);
            break;
        }
        writer.writeStructEnd();
    }
}
