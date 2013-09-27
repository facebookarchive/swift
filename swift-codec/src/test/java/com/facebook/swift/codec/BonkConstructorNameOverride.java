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

import javax.annotation.concurrent.Immutable;

@Immutable
@ThriftStruct("Bonk")
public final class BonkConstructorNameOverride
{
    private final String message;
    private final int type;

    @ThriftConstructor
    public BonkConstructorNameOverride(String message, int type)
    {
        this.message = message;
        this.type = type;
    }

    @ThriftField(value = 1, name = "myMessage")
    public String getMessage()
    {
        return message;
    }

    @ThriftField(value = 2, name = "myType")
    public int getType()
    {
        return type;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BonkConstructorNameOverride that = (BonkConstructorNameOverride) o;

        if (type != that.type) {
            return false;
        }
        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + type;
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("BonkConstructor");
        sb.append("{message='").append(message).append('\'');
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
