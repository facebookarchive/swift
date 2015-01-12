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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.codec.metadata.TypeCoercion;

public class CoercionHelper {
    // val logger = Logger.getLogger(this.getClass().getName());

    public static boolean isStaticMethod(Method m) {
        return (m.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0;
    }

    public static java.util.List<Method> getStaticMethods(Class<?> cls) {
        ArrayList<Method> result = new ArrayList<Method>();
        for (Method m : cls.getMethods()) {
            if (isStaticMethod(m)) {
                result.add(m);
            }
        }
        return result;
    }

    public static Type getCollectionParameterType(Type javaType, int index) {
        Type elementType = ((ParameterizedType) javaType).getActualTypeArguments()[index];

        if (elementType == null) {
            throw new IllegalArgumentException("Could not find collection parameter type " + index + " in " + javaType);
        }
        if (elementType == Object.class) {
            // Uh-oh. Probably some type erasure happened.
            throw new IllegalArgumentException("Thrift can't serialize type Object, type parameter  " + index + " of " + javaType);
        }

        return elementType;
    }

    //
    // TODO. Replace / add a method to scan for @ToThrift or @FromThrift methods
    // matching a given parameter type. Ordering is important because there are
    // many potential type matches for
    // generic and abstract types.
    //
    public static java.util.List<Method> getMethodsByName(Class<?> cls, String name) {
        ArrayList<Method> result = new ArrayList<Method>();
        for (Method m : cls.getMethods()) {
            if (m.getName() == name) {
                result.add(m);
            }
        }
        return result;
    }

    public static Method getMethodByName(Class<?> cls, String name) {

        List<Method> matched = getMethodsByName(cls, name);
        if (matched.size() == 1) {
            return matched.get(0);
        }
        if (matched.size() == 0) {
            return null;
        }
        throw new IllegalArgumentException("Multiple methods matching: " + cls.getName() + "." + name);
    }

    public static ThriftType makeCoercion(
            ThriftCatalog catalog,
            Type javaType,
            Class<?> coercionsMethodClass,
            String fromThriftMethodName,
            String toThriftMethodName,
            ThriftType serializedAs)
    {

        Method toThrift = CoercionHelper.getMethodByName(coercionsMethodClass, toThriftMethodName);
        if (toThrift == null) {
            throw new IllegalArgumentException("Could not find toThrift method: " + coercionsMethodClass.getName() + "." + toThriftMethodName);
        }

        Method fromThrift = CoercionHelper.getMethodByName(coercionsMethodClass, fromThriftMethodName);
        if (fromThrift == null) {
            throw new IllegalArgumentException("Could not find fromThrift method: " + coercionsMethodClass.getName() + "." + fromThriftMethodName);
        }

        ThriftType thriftType = ThriftType.coercion(javaType, serializedAs);

        TypeCoercion coercion = new TypeCoercion(thriftType, toThrift, fromThrift);
        catalog.addCoercion(coercion);
        // logger.debug(s"New coercion: thriftType:${thriftType}\n           javaType:${javaType}   uncoercedType=${serializedAs}")
        return thriftType;
    }

    public static ThriftType makeListCoercion(
            ThriftCatalog catalog,
            Type javaType,
            Class<?> coercionsMethodClass,
            String fromThriftMethodName,
            String toThriftMethodName)
    {
        ThriftType elementType = catalog.getThriftType(getCollectionParameterType(javaType, 0));
        return CoercionHelper.makeCoercion(
                catalog,
                javaType,
                coercionsMethodClass,
                fromThriftMethodName,
                toThriftMethodName,
                ThriftType.list(elementType));
    }

    public static ThriftType makeSetCoercion(
            ThriftCatalog catalog,
            Type javaType,
            Class<?> coercionsMethodClass,
            String fromThriftMethodName,
            String toThriftMethodName)
    {
        ThriftType elementType = catalog.getThriftType(getCollectionParameterType(javaType, 0));
        return CoercionHelper.makeCoercion(
                catalog,
                javaType,
                coercionsMethodClass,
                fromThriftMethodName,
                toThriftMethodName,
                ThriftType.set(elementType));
    }

    public static ThriftType makeMapCoercion(
            ThriftCatalog catalog,
            Type javaType,
            Class<?> coercionsMethodClass,
            String fromThriftMethodName,
            String toThriftMethodName)
    {
        ThriftType keyType = catalog.getThriftType(getCollectionParameterType(javaType, 0));
        ThriftType valueType = catalog.getThriftType(getCollectionParameterType(javaType, 1));

        return makeCoercion(
                catalog,
                javaType,
                coercionsMethodClass,
                fromThriftMethodName,
                toThriftMethodName,
                ThriftType.map(keyType, valueType)); // ThriftProtocolType.MAP,
    }
}