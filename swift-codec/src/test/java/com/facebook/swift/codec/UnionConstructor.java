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

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@ThriftUnion("Union")
public final class UnionConstructor
{
    private final Object value;
    private final short type;

    @ThriftConstructor
    public UnionConstructor(String stringValue)
    {
        this.value = stringValue;
        this.type = 1;
    }

    @ThriftConstructor
    public UnionConstructor(Long longValue)
    {
        this.value = longValue;
        this.type = 2;
    }

    @ThriftConstructor
    public UnionConstructor(Fruit fruitValue)
    {
        this.value = fruitValue;
        this.type = 3;
    }

    @ThriftUnionId
    public short getType()
    {
        return type;
    }

    @ThriftField(1)
    public String getStringValue()
    {
        if (type != 1) {
            throw new IllegalStateException("not a stringValue");
        }
        return (String) value;
    }

    @ThriftField(2)
    public Long getLongValue()
    {
        if (type != 2) {
            throw new IllegalStateException("not a longValue");
        }
        return (Long) value;
    }

    @ThriftField(3)
    public Fruit getFruitValue()
    {
        if (type != 3) {
            throw new IllegalStateException("not a fruitValue");
        }
        return (Fruit) value;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(value, type);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        UnionConstructor that = (UnionConstructor) obj;
        return Objects.equal(this.type, that.type)
            && Objects.equal(this.value, that.value);
    }

    @Override
    public String toString()
    {
        ToStringHelper helper = Objects.toStringHelper(this);

        if (type == 1) {
            helper.add("stringValue", (String) value);
        }
        else if (type == 2) {
            helper.add("longValue", (Long) value);
        }
        else if (type == 3) {
            helper.add("fruitValue", (Fruit) value);
        }
        return helper.toString();
    }
}
