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

package com.facebook.swift.codec.recursion;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftUnion;
import com.facebook.swift.codec.ThriftUnionId;

import java.util.Objects;

import static com.facebook.swift.codec.ThriftField.*;

@ThriftUnion
public class RecursiveDefaultUnion
{
    @ThriftUnionId
    public short unionId;

    public Object value;

    @ThriftConstructor
    public RecursiveDefaultUnion(RecursiveDefaultUnion child)
    {
        this.unionId = 1;
        this.value = child;
    }

    @ThriftConstructor
    public RecursiveDefaultUnion(String data)
    {
        this.unionId = 2;
        this.value = data;
    }

    @ThriftField(value = 1, requiredness = ThriftField.Requiredness.NONE, isRecursive = ThriftField.Recursiveness.TRUE)
    public RecursiveDefaultUnion getChild()
    {
        return (RecursiveDefaultUnion)value;
    }

    @ThriftField(2)
    public String getData()
    {
        return (String)value;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final RecursiveDefaultUnion that = (RecursiveDefaultUnion) obj;

        return Objects.equals(this.unionId, that.unionId) &&
                Objects.equals(this.value, that.value);
    }
}
