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
package com.facebook.swift.codec.metadata;

import com.facebook.swift.codec.ThriftProtocolType;

import java.lang.reflect.Type;
import java.util.Objects;

public class DefaultThriftTypeReference implements ThriftTypeReference
{
    private final ThriftType thriftType;

    public DefaultThriftTypeReference(ThriftType thriftType)
    {
        this.thriftType = thriftType;
    }

    @Override
    public Type getJavaType()
    {
        return thriftType.getJavaType();
    }

    @Override
    public ThriftProtocolType getProtocolType()
    {
        return thriftType.getProtocolType();
    }

    @Override
    public boolean isRecursive()
    {
        return false;
    }

    @Override
    public ThriftType get()
    {
        return thriftType;
    }

    @Override
    public String toString()
    {
        return "Reference to " + thriftType;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(thriftType.hashCode());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != DefaultThriftTypeReference.class) {
            return false;
        }

        DefaultThriftTypeReference that = (DefaultThriftTypeReference) obj;

        return Objects.equals(this.thriftType, that.thriftType);
    }
}
