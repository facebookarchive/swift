/*
 * Copyright 2012 Facebook, Inc.
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

import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class ThriftMethodExtractor implements ThriftExtraction
{
    private final short id;
    private final String name;
    private final Method method;

    public ThriftMethodExtractor(short id, String name, Method method)
    {
        checkArgument(id >= 0, "fieldId is negative");
        checkNotNull(name, "name is null");
        checkNotNull(method, "method is null");

        this.id = id;
        this.name = name;
        this.method = method;
    }

    @Override
    public short getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public Method getMethod()
    {
        return method;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftMethodExtractor");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", method=").append(method);
        sb.append('}');
        return sb.toString();
    }
}
