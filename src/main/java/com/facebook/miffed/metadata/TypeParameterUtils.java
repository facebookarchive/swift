/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.facebook.miffed.metadata;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public class TypeParameterUtils
{
    public static Type[] getTypeParameters(Class<?> desiredType, Type type)
    {
        if (type instanceof Class) {
            Class<?> rawClass = (Class<?>) type;

            // if this is the collection class we're done
            if (desiredType.equals(type)) {
                return null;
            }

            for (Type iface : rawClass.getGenericInterfaces()) {
                Type[] collectionType = getTypeParameters(desiredType, iface);
                if (collectionType != null) {
                    return collectionType;
                }
            }

            return getTypeParameters(desiredType, rawClass.getGenericSuperclass());
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            Type rawType = parameterizedType.getRawType();
            if (desiredType.equals(rawType)) {
                return parameterizedType.getActualTypeArguments();
            }

            Type[] collectionTypes = getTypeParameters(desiredType, rawType);
            if (collectionTypes != null) {
                for (int i = 0; i < collectionTypes.length; i++) {
                    if (collectionTypes[i] instanceof TypeVariable) {
                        TypeVariable<?> typeVariable = (TypeVariable<?>) collectionTypes[i];
                        TypeVariable<?>[] rawTypeParams = ((Class<?>) rawType).getTypeParameters();
                        for (int j = 0; j < rawTypeParams.length; j++) {
                            if (typeVariable.getName().equals(rawTypeParams[j].getName())) {
                                collectionTypes[i] = parameterizedType.getActualTypeArguments()[j];
                            }
                        }
                    }
                }
            }
            return collectionTypes;
        }
        return null;
    }

    /*
     * Copyright (C) 2011 The Guava Authors
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    public static Class<?> getRawType(Type type)
    {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            // JDK implementation declares getRawType() to return Class<?>
            return (Class<?>) parameterizedType.getRawType();
        }
        else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            return getArrayClass(getRawType(genericArrayType.getGenericComponentType()));
        }
        else if (type instanceof TypeVariable) {
            // First bound is always the "primary" bound that determines the runtime signature.
            return getRawType(((TypeVariable<?>) type).getBounds()[0]);
        }
        else if (type instanceof WildcardType) {
            // Wildcard can have one and only one upper bound.
            return getRawType(((WildcardType) type).getUpperBounds()[0]);
        }
        else {
            throw new AssertionError(type + " unsupported");
        }
    }

    private static Class<?> getArrayClass(Class<?> componentType)
    {
        // TODO(user): This is not the most efficient way to handle generic
        // arrays, but is there another way to extract the array class in a
        // non-hacky way (i.e. using String value class names- "[L...")?
        return Array.newInstance(componentType, 0).getClass();
    }
}
