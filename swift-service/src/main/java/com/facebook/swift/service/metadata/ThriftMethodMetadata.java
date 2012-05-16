/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.metadata;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;
import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftFieldMetadata;
import com.facebook.swift.codec.metadata.ThriftInjection;
import com.facebook.swift.codec.metadata.ThriftParameterInjection;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.facebook.swift.codec.metadata.ReflectionHelper.extractParameterNames;

@Immutable
public class ThriftMethodMetadata {
  private final String name;
  private final ThriftType returnType;
  private final List<ThriftFieldMetadata> parameters;
  private final Method method;
  private final ImmutableMap<Short,ThriftType> exceptions;

  public ThriftMethodMetadata(Method method, ThriftCatalog catalog) {
    Preconditions.checkNotNull(method, "method is null");
    Preconditions.checkNotNull(catalog, "catalog is null");

    this.method = method;
    
    ThriftMethod thriftMethod = method.getAnnotation(ThriftMethod.class);
    Preconditions.checkArgument(thriftMethod != null, "Method is not annotated with @ThriftMethod");

    Preconditions.checkArgument(
        !Modifier.isStatic(method.getModifiers()),
        "Method %s is static", method.toGenericString()
    );

    if (thriftMethod.value().length() == 0) {
      name = method.getName();
    } else {
      name = thriftMethod.value();
    }

    returnType = catalog.getThriftType(method.getGenericReturnType());

    ImmutableList.Builder<ThriftFieldMetadata> builder = ImmutableList.builder();
    Type[] parameterTypes = method.getGenericParameterTypes();
    String[] parameterNames = extractParameterNames(method);
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    for (int index = 0; index < parameterTypes.length; index++) {
      ThriftField thriftField = null;
      for (Annotation annotation : parameterAnnotations[index]) {
        if (annotation instanceof ThriftField) {
          thriftField = (ThriftField) annotation;
          break;
        }
      }

      short parameterId = Short.MIN_VALUE;
      String parameterName = null;
      if (thriftField != null) {
        parameterId = thriftField.value();
        if (!thriftField.name().isEmpty()) {
          parameterName = thriftField.name();
        }
      }
      if (parameterId == Short.MIN_VALUE) {
        parameterId = (short) (index + 1);
      }
      if (parameterName == null) {
        parameterName = parameterNames[index];
      }

      Type parameterType = parameterTypes[index];

      ThriftType thriftType = catalog.getThriftType(parameterType);

      ThriftFieldMetadata fieldMetadata = new ThriftFieldMetadata(
          parameterId,
          thriftType,
          parameterName,
          ImmutableList.<ThriftInjection>of(
              new ThriftParameterInjection(
                  parameterId,
                  parameterName,
                  index,
                  parameterType
              )
          ),
          null,
          null
      );
      builder.add(fieldMetadata);
    }
    parameters = builder.build();

    ImmutableMap.Builder<Short, ThriftType> exceptions = ImmutableMap.builder();
    if (thriftMethod.exception().length > 0) {
      for (ThriftException thriftException : thriftMethod.exception()) {
        exceptions.put(thriftException.id(), catalog.getThriftType(thriftException.type()));
      }
    } else if (method.getExceptionTypes().length == 1) {
      Class<?> exceptionClass = method.getExceptionTypes()[0];
      if (exceptionClass.isAnnotationPresent(ThriftStruct.class)) {
        exceptions.put((short) 1, catalog.getThriftType(exceptionClass));
      }
    }
    this.exceptions = exceptions.build();
  }

  public String getName() {
    return name;
  }

  public ThriftType getReturnType() {
    return returnType;
  }

  public List<ThriftFieldMetadata> getParameters() {
    return parameters;
  }

  public Map<Short, ThriftType> getExceptions() {
    return exceptions;
  }

  public ThriftType getException(short id) {
    return exceptions.get(id);
  }

  public Method getMethod() {
    return method;
  }
}
