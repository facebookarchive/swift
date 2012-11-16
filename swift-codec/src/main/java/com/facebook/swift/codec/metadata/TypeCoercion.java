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

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Method;

@Immutable
public class TypeCoercion
{
    private final ThriftType thriftType;
    private final Method toThrift;
    private final Method fromThrift;

    public TypeCoercion(ThriftType thriftType, Method toThrift, Method fromThrift)
    {
        Preconditions.checkNotNull(thriftType, "thriftType is null");
        Preconditions.checkNotNull(toThrift, "toThrift is null");
        Preconditions.checkNotNull(fromThrift, "fromThrift is null");

        this.thriftType = thriftType;
        this.toThrift = toThrift;
        this.fromThrift = fromThrift;
    }

    public ThriftType getThriftType()
    {
        return thriftType;
    }

    public Method getToThrift()
    {
        return toThrift;
    }

    public Method getFromThrift()
    {
        return fromThrift;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("TypeCoercion");
        sb.append("{thriftType=").append(thriftType);
        sb.append(", toThrift=").append(toThrift);
        sb.append(", fromThrift=").append(fromThrift);
        sb.append('}');
        return sb.toString();
    }
}
