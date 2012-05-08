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
package com.facebook.swift.codec.metadata;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;

final class ReflectionHelper {
  private ReflectionHelper() {
  }

  private static final Type MAP_KEY_TYPE;
  private static final Type MAP_VALUE_TYPE;
  private static final Type ITERATOR_TYPE;
  private static final Type ITERATOR_ELEMENT_TYPE;

  static {
    try {
      Method mapPutMethod = Map.class.getMethod("put", Object.class, Object.class);
      MAP_KEY_TYPE = mapPutMethod.getGenericParameterTypes()[0];
      MAP_VALUE_TYPE = mapPutMethod.getGenericParameterTypes()[1];

      ITERATOR_TYPE = Iterable.class.getMethod("iterator").getGenericReturnType();
      ITERATOR_ELEMENT_TYPE = Iterator.class.getMethod("next").getGenericReturnType();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

  }

  public static Type getMapKeyType(Type type) {
    return TypeToken.of(type).resolveType(MAP_KEY_TYPE).getType();
  }

  public static Type getMapValueType(Type type) {
    return TypeToken.of(type).resolveType(MAP_VALUE_TYPE).getType();
  }

  public static Type getIterableType(Type type) {
    return TypeToken.of(type).resolveType(ITERATOR_TYPE).resolveType(ITERATOR_ELEMENT_TYPE).getType();
  }

  public static Iterable<Method> getAllDeclaredMethods(Class<?> type) {
    ImmutableList.Builder<Method> methods = ImmutableList.builder();

    for (Class<?> clazz = type;
         (clazz != null) && !clazz.equals(Object.class);
         clazz = clazz.getSuperclass()) {

      methods.addAll(ImmutableList.copyOf(clazz.getDeclaredMethods()));
    }
    return methods.build();
  }

  public static Iterable<Field> getAllDeclaredFields(Class<?> type) {
    ImmutableList.Builder<Field> fields = ImmutableList.builder();
    for (Class<?> clazz = type;
         (clazz != null) && !clazz.equals(Object.class);
         clazz = clazz.getSuperclass()) {
      fields.addAll(ImmutableList.copyOf(clazz.getDeclaredFields()));
    }
    return fields.build();
  }

  /**
   * Find methods that are tagged with a given annotation somewhere in the hierarchy
   */
  public static Collection<Method> findAnnotatedMethods(
      Class<?> type,
      Class<? extends Annotation> annotation
  ) {

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
          method.getParameterTypes()
      );
      if (managedMethod != null) {
        result.add(managedMethod);
      }
    }

    return result;
  }

  public static Method findAnnotatedMethod(
      Class<?> configClass,
      Class<? extends Annotation> annotation,
      String methodName,
      Class<?>... paramTypes
  ) {
    try {
      Method method = configClass.getDeclaredMethod(methodName, paramTypes);
      if (method != null && method.isAnnotationPresent(annotation)) {
        return method;
      }
    } catch (NoSuchMethodException e) {
      // ignore
    }

    if (configClass.getSuperclass() != null) {
      Method managedMethod = findAnnotatedMethod(
          configClass.getSuperclass(),
          annotation,
          methodName,
          paramTypes
      );
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

  public static Collection<Field> findAnnotatedFields(
      Class<?> type,
      Class<? extends Annotation> annotation
  ) {
    List<Field> result = new ArrayList<>();

    // gather all publicly available methods
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
}
