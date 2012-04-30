/*
 * Copyright 2010 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.swift.metadata;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReflectionHelper
{
    public static Iterable<Method> getAllDeclaredMethods(Class<?> type)
    {
        ImmutableList.Builder<Method> methods = ImmutableList.builder();
        for (Class<?> clazz = type; (clazz != null) && !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
            methods.addAll(ImmutableList.copyOf(clazz.getDeclaredMethods()));
        }
        return methods.build();
    }

    public static Iterable<Field> getAllDeclaredFields(Class<?> type)
    {
        ImmutableList.Builder<Field> fields = ImmutableList.builder();
        for (Class<?> clazz = type; (clazz != null) && !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
            fields.addAll(ImmutableList.copyOf(clazz.getDeclaredFields()));
        }
        return fields.build();
    }

    /**
     * Find methods that are tagged with a given annotation somewhere in the hierarchy
     */
    public static Collection<Method> findAnnotatedMethods(Class<?> type, Class<? extends Annotation> annotation)
    {
        List<Method> result = new ArrayList<Method>();

        // gather all publicly available methods
        // this returns everything, even if it's declared in a parent
        for (Method method : type.getMethods()) {
            // skip methods that are used internally by the vm for implementing covariance, etc
            if (method.isSynthetic() || method.isBridge() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            // look for annotations recursively in super-classes or interfaces
            Method managedMethod = findAnnotatedMethod(type, annotation, method.getName(), method.getParameterTypes());
            if (managedMethod != null) {
                result.add(managedMethod);
            }
        }

        return result;
    }

    public static Method findAnnotatedMethod(Class<?> configClass, Class<? extends Annotation> annotation, String methodName, Class<?>... paramTypes)
    {
        try {
            Method method = configClass.getDeclaredMethod(methodName, paramTypes);
            if (method != null && method.isAnnotationPresent(annotation)) {
                return method;
            }
        }
        catch (NoSuchMethodException e) {
            // ignore
        }

        if (configClass.getSuperclass() != null) {
            Method managedMethod = findAnnotatedMethod(configClass.getSuperclass(), annotation, methodName, paramTypes);
            if (managedMethod != null) {
                return managedMethod;
            }
        }

        for (Class<?> iface : configClass.getInterfaces()) {
            Method managedMethod = findAnnotatedMethod(iface, annotation, methodName, paramTypes);
            if (managedMethod != null) {
                return managedMethod;
            }
        }

        return null;
    }

    public static Collection<Field> findAnnotatedFields(Class<?> type, Class<? extends Annotation> annotation)
    {
        List<Field> result = new ArrayList<>();

        // gather all publicly available methods
        // this returns everything, even if it's declared in a parent
        for (Field field : type.getFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            if (field.isAnnotationPresent(annotation)) {
                result.add(field);
            }
        }

        return result;
    }
}
