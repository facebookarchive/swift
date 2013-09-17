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

import com.google.common.base.Joiner;

import javax.annotation.concurrent.Immutable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class ThriftMethodInjection
{
    private final Method method;
    private final List<ThriftParameterInjection> parameters;

    public ThriftMethodInjection(Method method, ThriftParameterInjection ... parameters)
    {
        this(method, Arrays.asList(parameters));
    }

    public ThriftMethodInjection(Method method, List<ThriftParameterInjection> parameters)
    {
        checkNotNull(method, "method is null");
        checkNotNull(parameters, "parameters is null");

        this.method = method;
        this.parameters = parameters;
    }

    public Method getMethod()
    {
        return method;
    }

    public List<ThriftParameterInjection> getParameters()
    {
        return parameters;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(method.getName());
        sb.append('(');
        Joiner.on(", ").appendTo(sb, parameters);
        sb.append(')');
        return sb.toString();
    }
}
