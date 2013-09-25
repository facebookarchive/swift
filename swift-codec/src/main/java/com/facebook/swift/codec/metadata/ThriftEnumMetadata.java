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

import com.facebook.swift.codec.ThriftEnumValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import static java.lang.String.format;

@Immutable
public class ThriftEnumMetadata<T extends Enum<T>>
{
    private final Class<T> enumClass;
    private final Map<Integer, T> byEnumValue;
    private final Map<T, Integer> byEnumConstant;
    private final String enumName;
    private final ImmutableList<String> documentation;
    private final ImmutableMap<T, ImmutableList<String>> elementDocs;

    public ThriftEnumMetadata(
            String enumName,
            Class<T> enumClass)
            throws RuntimeException
    {
        Preconditions.checkNotNull(enumName, "enumName must not be null");
        Preconditions.checkNotNull(enumClass, "enumClass must not be null");

        this.enumName = enumName;
        this.enumClass = enumClass;

        Method enumValueMethod = null;
        for (Method method : enumClass.getMethods()) {
            if (method.isAnnotationPresent(ThriftEnumValue.class)) {
                Preconditions.checkArgument(
                        Modifier.isPublic(method.getModifiers()),
                        "Enum class %s @ThriftEnumValue method is not public: %s",
                        enumClass.getName(),
                        method);
                Preconditions.checkArgument(
                        !Modifier.isStatic(method.getModifiers()),
                        "Enum class %s @ThriftEnumValue method is static: %s",
                        enumClass.getName(),
                        method);
                Preconditions.checkArgument(
                        method.getTypeParameters().length == 0,
                        "Enum class %s @ThriftEnumValue method has parameters: %s",
                        enumClass.getName(),
                        method);
                Class<?> returnType = method.getReturnType();
                Preconditions.checkArgument(
                        returnType == int.class || returnType == Integer.class,
                        "Enum class %s @ThriftEnumValue method does not return int or Integer: %s",
                        enumClass.getName(),
                        method);
                enumValueMethod = method;
            }
        }

        ImmutableMap.Builder<T, ImmutableList<String>> elementDocs = ImmutableMap.builder();
        if (enumValueMethod != null) {
            ImmutableMap.Builder<Integer, T> byEnumValue = ImmutableMap.builder();
            ImmutableMap.Builder<T, Integer> byEnumConstant = ImmutableMap.builder();
            for (T enumConstant : enumClass.getEnumConstants()) {
                Integer value;
                try {
                    value = (Integer) enumValueMethod.invoke(enumConstant);
                }
                catch (Exception e) {
                    throw new RuntimeException(format("Enum class %s element %s get value method threw an exception", enumClass.getName(), enumConstant), e);
                }
                Preconditions.checkArgument(
                        value != null,
                        "Enum class %s element %s returned null for enum value: %s",
                        enumClass.getName(),
                        enumConstant
                );

                byEnumValue.put(value, enumConstant);
                byEnumConstant.put(enumConstant, value);
                elementDocs.put(enumConstant, ThriftCatalog.getThriftDocumentation(enumConstant));
            }
            this.byEnumValue = byEnumValue.build();
            this.byEnumConstant = byEnumConstant.build();
        }
        else {
            byEnumValue = null;
            byEnumConstant = null;
            for (T enumConstant : enumClass.getEnumConstants()) {
                elementDocs.put(enumConstant, ThriftCatalog.getThriftDocumentation(enumConstant));
            }
        }
        this.elementDocs = elementDocs.build();
        this.documentation = ThriftCatalog.getThriftDocumentation(enumClass);
    }

    public String getEnumName()
    {
        return enumName;
    }

    public Class<T> getEnumClass()
    {
        return enumClass;
    }

    public boolean hasExplicitThriftValue()
    {
        return byEnumValue != null;
    }

    public Map<Integer, T> getByEnumValue()
    {
        return byEnumValue;
    }

    public Map<T, Integer> getByEnumConstant()
    {
        return byEnumConstant;
    }

    public ImmutableList<String> getDocumentation()
    {
        return documentation;
    }

    public Map<T, ImmutableList<String>> getElementsDocumentation()
    {
        return elementDocs;
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

        final ThriftEnumMetadata<?> that = (ThriftEnumMetadata<?>) o;

        if (!enumClass.equals(that.enumClass)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return enumClass.hashCode();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThriftEnumMetadata");
        sb.append("{enumClass=").append(enumClass);
        sb.append(", byThriftValue=").append(byEnumValue);
        sb.append('}');
        return sb.toString();
    }
}
