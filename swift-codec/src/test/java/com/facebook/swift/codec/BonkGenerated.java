/*
 * Copyright (C) 2015 Facebook, Inc.
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

@ThriftStruct(value = "BonkGenerated", builder = Generated_BonkGenerated.Builder.class)
public abstract class BonkGenerated
{

    public static Builder builder()
    {
        return new Generated_BonkGenerated.Builder();
    }

    @ThriftField(value = 1)
    public abstract String message();

    @ThriftField(value = 2)
    public abstract int type();

    public interface Builder
    {
        @ThriftField
        Builder message(String message);

        @ThriftField
        Builder type(int type);

        @ThriftConstructor
        BonkGenerated build();
    }
}

final class Generated_BonkGenerated extends BonkGenerated
{

    private final String message;
    private final int type;

    private Generated_BonkGenerated(String message, int type)
    {
        this.message = message;
        this.type = type;
    }

    @ThriftField(value = 1)
    @Override
    public String message()
    {
        return message;
    }

    @ThriftField(value = 2)
    @Override
    public int type()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return "BonkGenerated{"
            + "message=" + message + ", "
            + "type=" + type
            + "}";
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }
        if (o instanceof BonkGenerated) {
            BonkGenerated that = (BonkGenerated) o;
            return ((this.message == null) ? (that.message() == null) : this.message.equals(that.message()))
                && (this.type == that.type());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int h = 1;
        h *= 1000003;
        h ^= (message == null) ? 0 : message.hashCode();
        h *= 1000003;
        h ^= type;
        return h;
    }

    static final class Builder implements BonkGenerated.Builder
    {
        private String message;
        private int type;

        @Override
        public BonkGenerated.Builder message(String message)
        {
            this.message = message;
            return this;
        }

        @Override
        public BonkGenerated.Builder type(int type)
        {
            this.type = type;
            return this;
        }

        @Override
        public BonkGenerated build()
        {
            return new Generated_BonkGenerated(message, type);
        }
    }
}
