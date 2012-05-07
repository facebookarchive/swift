/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.metadata;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class TypeParameterUtils {
  /**
   * Licensed to the Apache Software Foundation (ASF) under one or more
   * contributor license agreements.  See the NOTICE file distributed with
   * this work for additional information regarding copyright ownership.
   * The ASF licenses this file to You under the Apache License, Version 2.0
   * (the "License"); you may not use this file except in compliance with
   * the License.  You may obtain a copy of the License at
   * <p/>
   * http://www.apache.org/licenses/LICENSE-2.0
   * <p/>
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
  // http://geronimo.apache.org/maven/xbean/3.6/xbean-reflect/apidocs/src-html/org/apache/xbean/recipe/RecipeHelper.html
  public static Type[] getTypeParameters(Class<?> desiredType, Type type) {
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
}
