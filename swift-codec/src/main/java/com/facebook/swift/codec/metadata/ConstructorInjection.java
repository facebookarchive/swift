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

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Constructor;
import java.util.List;

class ConstructorInjection
{
    private final Constructor<?> constructor;
    private final List<ParameterInjection> parameters;

    public ConstructorInjection(Constructor<?> constructor, List<ParameterInjection> parameters)
    {
        this.constructor = constructor;
        this.parameters = ImmutableList.copyOf(parameters);
    }

    public ConstructorInjection(Constructor<?> constructor, ParameterInjection... parameters)
    {
        this.constructor = constructor;
        this.parameters = ImmutableList.copyOf(parameters);
    }

    public Constructor<?> getConstructor()
    {
        return constructor;
    }

    public List<ParameterInjection> getParameters()
    {
        return parameters;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ConstructorInjection");
        sb.append("{constructor=").append(constructor);
        sb.append(", parameters=").append(parameters);
        sb.append('}');
        return sb.toString();
    }
}
