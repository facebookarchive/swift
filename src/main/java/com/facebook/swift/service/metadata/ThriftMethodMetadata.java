/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.metadata;

import com.facebook.swift.metadata.ThriftCatalog;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

public class ThriftMethodMetadata {
  private final String name;
  private final ThriftType returnType;
  private final List<ThriftType> parameterTypes;
  private final Method method;

  public ThriftMethodMetadata(String name, Method method, ThriftCatalog catalog) {
    Preconditions.checkNotNull(name, "name is null");
    Preconditions.checkNotNull(method, "method is null");
    Preconditions.checkNotNull(catalog, "catalog is null");

    this.name = name;
    this.method = method;

    returnType = catalog.getThriftType(method.getGenericReturnType());

    ImmutableList.Builder<ThriftType> builder = ImmutableList.builder();
    for (Type type : method.getGenericParameterTypes()) {
      builder.add(catalog.getThriftType(type));
    }
    parameterTypes = builder.build();
  }

  public String getName() {
    return name;
  }

  public ThriftType getReturnType() {
    return returnType;
  }

  public List<ThriftType> getParameterTypes() {
    return parameterTypes;
  }

  public Method getMethod() {
    return method;
  }
}
