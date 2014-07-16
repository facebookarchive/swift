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

public enum ThriftProtocolType
{
    UNKNOWN((byte) 0),
    BOOL((byte) 2),
    BYTE((byte) 3),
    DOUBLE((byte) 4),
    I16((byte) 6),
    I32((byte) 8),
    I64((byte) 10),
    STRING((byte) 11),
    STRUCT((byte) 12),
    MAP((byte) 13),
    SET((byte) 14),
    LIST((byte) 15),
    ENUM((byte) 8), // same as I32 type
    BINARY((byte) 11); // same as STRING type

    private final byte type;

    private ThriftProtocolType(byte type)
    {
        this.type = type;
    }

    public byte getType()
    {
        return type;
    }
}
