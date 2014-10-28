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
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.thoughtworks.paranamer.AdaptiveParanamer;
import com.thoughtworks.paranamer.AnnotationParanamer;
import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.CachingParanamer;
import com.thoughtworks.paranamer.Paranamer;

import javax.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.reflect.Modifier.isStatic;

public final class ReflectionHelper
{
    private ReflectionHelper()
    {
    }

    private static final Type MAP_KEY_TYPE;
    private static final Type MAP_VALUE_TYPE;
    private static final Type ITERATOR_TYPE;
    private static final Type ITERATOR_ELEMENT_TYPE;
    private static final Type FUTURE_RETURN_TYPE;

    static {
        try {
            Method mapPutMethod = Map.class.getMethod("put", Object.class, Object.class);
            MAP_KEY_TYPE = mapPutMethod.getGenericParameterTypes()[0];
            MAP_VALUE_TYPE = mapPutMethod.getGenericParameterTypes()[1];

            ITERATOR_TYPE = Iterable.class.getMethod("iterator").getGenericReturnType();
            ITERATOR_ELEMENT_TYPE = Iterator.class.getMethod("next").getGenericReturnType();

            Method futureGetMethod = Future.class.getMethod("get");
            FUTURE_RETURN_TYPE = futureGetMethod.getGenericReturnType();
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static boolean isArray(Type type)
    {
        return TypeToken.of(type).getComponentType() != null;
    }

    public static Class<?> getArrayOfType(Type componentType)
    {
        // this creates an extra object but is the simplest way to get an array class
        Class<?> rawComponentType = TypeToken.of(componentType).getRawType();
        return Array.newInstance(rawComponentType, 0).getClass();
    }

    public static Type getMapKeyType(Type type)
    {
        return TypeToken.of(type).resolveType(MAP_KEY_TYPE).getType();
    }

    public static Type getMapValueType(Type type)
    {
        return TypeToken.of(type).resolveType(MAP_VALUE_TYPE).getType();
    }

    public static Type getIterableType(Type type)
    {
        return TypeToken.of(type).resolveType(ITERATOR_TYPE).resolveType(ITERATOR_ELEMENT_TYPE).getType();
    }

    public static Type getFutureReturnType(Type type)
    {
        return TypeToken.of(type).resolveType(FUTURE_RETURN_TYPE).getType();
    }

    public static <T extends Annotation> Set<T> getEffectiveClassAnnotations(Class<?> type, Class<T> annotation)
    {
        // if the class is directly annotated, it is considered the only annotation
        if (type.isAnnotationPresent(annotation)) {
            return ImmutableSet.of(type.getAnnotation(annotation));
        }

        // otherwise find all annotations from all super classes and interfaces
        ImmutableSet.Builder<T> builder = ImmutableSet.builder();
        addEffectiveClassAnnotations(type, annotation, builder);
        return builder.build();
    }

    private static <T extends Annotation> void addEffectiveClassAnnotations(Class<?> type, Class<T> annotation, ImmutableSet.Builder<T> builder)
    {
        if (type.isAnnotationPresent(annotation)) {
            builder.add(type.getAnnotation(annotation));
            return;
        }
        if (type.getSuperclass() != null) {
            addEffectiveClassAnnotations(type.getSuperclass(), annotation, builder);
        }
        for (Class<?> anInterface : type.getInterfaces()) {
            addEffectiveClassAnnotations(anInterface, annotation, builder);
        }
    }

    public static Iterable<Method> getAllDeclaredMethods(Class<?> type)
    {
        ImmutableList.Builder<Method> methods = ImmutableList.builder();

        for (Class<?> clazz = type; clazz != null && !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
            methods.addAll(ImmutableList.copyOf(clazz.getDeclaredMethods()));
        }
        return methods.build();
    }

    public static Iterable<Field> getAllDeclaredFields(Class<?> type)
    {
        ImmutableList.Builder<Field> fields = ImmutableList.builder();
        for (Class<?> clazz = type; clazz != null && !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
            fields.addAll(ImmutableList.copyOf(clazz.getDeclaredFields()));
        }
        return fields.build();
    }

    /**
     * Find methods that are tagged with a given annotation somewhere in the hierarchy
     */
    public static Collection<Method> findAnnotatedMethods(Class<?> type, Class<? extends Annotation> annotation)
    {
        List<Method> result = new ArrayList<>();

        // gather all publicly available methods
        // this returns everything, even if it's declared in a parent
        for (Method method : type.getMethods()) {
            // skip methods that are used internally by the vm for implementing covariance, etc
            if (method.isSynthetic() || method.isBridge() || isStatic(method.getModifiers())) {
                continue;
            }

            // look for annotations recursively in super-classes or interfaces
            Method managedMethod = findAnnotatedMethod(
                    type,
                    annotation,
                    method.getName(),
                    method.getParameterTypes());
            if (managedMethod != null) {
                result.add(managedMethod);
            }
        }

        return result;
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
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
            Method managedMethod = findAnnotatedMethod(
                    configClass.getSuperclass(),
                    annotation,
                    methodName,
                    paramTypes);
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

        // gather all publicly available fields
        // this returns everything, even if it's declared in a parent
        for (Field field : type.getFields()) {
            if (field.isSynthetic() || isStatic(field.getModifiers())) {
                continue;
            }

            if (field.isAnnotationPresent(annotation)) {
                result.add(field);
            }
        }

        return result;
    }

    private static final Paranamer PARANAMER = new CachingParanamer(
            new AdaptiveParanamer(
                    new ThriftFieldParanamer(),
                    new BytecodeReadingParanamer(),
                    new GeneralParanamer()));

    public static String[] extractParameterNames(AccessibleObject methodOrConstructor)
    {
        String[] names = PARANAMER.lookupParameterNames(methodOrConstructor);
        return names;
    }

    private static class ThriftFieldParanamer extends AnnotationParanamer
    {
        @Override
        protected String getNamedValue(Annotation annotation)
        {
            if (annotation instanceof ThriftField) {
                String name = ((ThriftField) annotation).name();
                if (!name.isEmpty()) {
                    return name;
                }
            }
            return super.getNamedValue(annotation);
        }

        @Override
        protected boolean isNamed(Annotation annotation)
        {
            return annotation instanceof ThriftField || super.isNamed(annotation);
        }
    }

    private static class GeneralParanamer implements Paranamer
    {
        @Override
        public String[] lookupParameterNames(AccessibleObject methodOrConstructor)
        {
            String[] names;
            if (methodOrConstructor instanceof Method) {
                Method method = (Method) methodOrConstructor;
                names = new String[method.getParameterTypes().length];
            }
            else if (methodOrConstructor instanceof Constructor<?>) {
                Constructor<?> constructor = (Constructor<?>) methodOrConstructor;
                names = new String[constructor.getParameterTypes().length];
            }
            else {
                throw new IllegalArgumentException("methodOrConstructor is not an instance of Method or Constructor but is " + methodOrConstructor.getClass().getName());
            }
            for (int i = 0; i < names.length; i++) {
                names[i] = "arg" + i;
            }
            return names;
        }

        @Override
        public String[] lookupParameterNames(AccessibleObject methodOrConstructor, boolean throwExceptionIfMissing)
        {
            return lookupParameterNames(methodOrConstructor);
        }
    }

    public static String extractFieldName(Method method)
    {
        checkNotNull(method, "method is null");
        return extractFieldName(method.getName());
    }

    public static String extractFieldName(String methodName)
    {
        checkNotNull(methodName, "methodName is null");
        if ((methodName.startsWith("get") || methodName.startsWith("set")) && methodName.length() > 3) {
            String name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            return name;
        }
        else if (methodName.startsWith("is") && methodName.length() > 2) {
            String name = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            return name;
        }
        else {
            return methodName;
        }
    }

    public static Type resolveFieldType(Type structType, Type genericType)
    {
        return TypeToken.of(structType).resolveType(genericType).getType();
    }

    public static Type[] resolveFieldTypes(final Type structType, Type[] genericTypes)
    {
        return Lists.transform(Arrays.asList(genericTypes), new Function<Type, Type>()
        {
            @Nullable
            @Override
            public Type apply(@Nullable Type input)
            {
                return resolveFieldType(structType, input);
            }
        }).toArray(new Type[0]);
    }
}
