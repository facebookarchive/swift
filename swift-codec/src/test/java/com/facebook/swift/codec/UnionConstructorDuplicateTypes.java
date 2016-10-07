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

import com.facebook.swift.codec.ThriftField.Requiredness;

import java.util.Arrays;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

@ThriftUnion("UnionConstructorDuplicateTypes")
public class UnionConstructorDuplicateTypes {
    private Object value;
    private short id = -1;
    private String name;

    @ThriftConstructor
    public UnionConstructorDuplicateTypes() {
    }

    @ThriftField
    public void setFirstIntValue(final int firstIntValue) {
        this.value = firstIntValue;
        this.id = 1;
        this.name = "firstIntValue";
    }

    @ThriftField
    public void setSecondIntValue(final int secondIntValue) {
        this.value = secondIntValue;
        this.id = 2;
        this.name = "secondIntValue";
    }

    @ThriftField(value = 1, name = "firstIntValue", requiredness = Requiredness.NONE)
    public int getFirstIntValue() {
        if (this.id != 1) {
            throw new IllegalStateException("Not a firstIntValue element!");
        }
        return (int) value;
    }

    public boolean isSetFirstIntValue() {
        return this.id == 1;
    }

    @ThriftField(value = 2, name = "secondIntValue", requiredness = Requiredness.NONE)
    public int getSecondIntValue() {
        if (this.id != 2) {
            throw new IllegalStateException("Not a secondIntValue element!");
        }
        return (int) value;
    }

    public boolean isSetSecondIntValue() {
        return this.id == 2;
    }

    @ThriftUnionId
    public short getThriftId() {
        return this.id;
    }

    public String getThriftName() {
        return this.name;
    }

    @Override
    public int hashCode()
    {
        return Arrays.deepHashCode(new Object[] {
                id,
                value,
                name
        });
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        UnionConstructorDuplicateTypes that = (UnionConstructorDuplicateTypes) obj;
        return Objects.equals(this.id, that.id)
                && Objects.equals(this.value, that.value)
                && Objects.equals(this.name, that.name);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("value", value)
                .add("id", id)
                .add("name", name)
                .add("type", value == null ? "<null>" : value.getClass().getSimpleName())
                .toString();
    }
}
