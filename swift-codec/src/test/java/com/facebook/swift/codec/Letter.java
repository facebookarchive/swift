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

public enum Letter
{
    A(65), B(66), C(67), D(68);

    private final int asciiValue;

    Letter(int asciiValue)
    {

        this.asciiValue = asciiValue;
    }

    @ThriftEnumValue
    public int getAsciiValue()
    {
        return asciiValue;
    }
}
