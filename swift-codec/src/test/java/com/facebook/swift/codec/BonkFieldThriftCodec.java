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

public class BonkFieldThriftCodec implements ThriftCodec<BonkField>
{
    private final ThriftType type;

    public BonkFieldThriftCodec(ThriftType type)
    {
        this.type = type;
    }

    @Override
    public ThriftType getType()
    {
        return type;
    }

    @Override
    public BonkField read(TProtocol protocol)
            throws Exception
    {
        TProtocolReader reader = new TProtocolReader(protocol);

        String message = null;
        int type = 0;

        reader.readStructBegin();

        while (reader.nextField()) {
            switch (reader.getFieldId()) {
                case 1:
                    message = reader.readStringField();
                    break;
                case 2:
                    type = reader.readI32Field();
                    break;
                default:
                    reader.skipFieldData();
            }
        }
        reader.readStructEnd();

        BonkField bonkField = new BonkField();
        if (message != null) {
            bonkField.message = message;
        }
        bonkField.type = type;

        return bonkField;
    }

    @Override
    public void write(BonkField value, TProtocol protocol)
            throws Exception
    {
        TProtocolWriter writer = new TProtocolWriter(protocol);

        writer.writeStructBegin("bonk");

        String message = value.message;
        if (message != null) {
            writer.writeStringField("message", (short) 1, message);
        }

        writer.writeI32Field("type", (short) 2, value.type);
        writer.writeStructEnd();
    }
}
