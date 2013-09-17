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

import com.facebook.swift.codec.ThriftField;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static com.facebook.swift.codec.metadata.ReflectionHelper.extractFieldName;

class MethodExtractor extends Extractor
{
    private final Method method;

    public MethodExtractor(Method method, ThriftField annotation, FieldType fieldType)
    {
        super(annotation, fieldType);
        this.method = method;
    }

    public Method getMethod()
    {
        return method;
    }

    @Override
    public String extractName()
    {
        return extractFieldName(method.getName());
    }

    @Override
    public Type getJavaType()
    {
        return method.getGenericReturnType();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("MethodExtractor");
        sb.append("{method=").append(method);
        sb.append('}');
        return sb.toString();
    }
}
