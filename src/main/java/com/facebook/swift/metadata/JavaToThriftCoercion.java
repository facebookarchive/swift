/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import com.google.common.base.Preconditions;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class JavaToThriftCoercion {
  private final Type javaType;
  private final ThriftType thriftType;
  private final Method method;

  public JavaToThriftCoercion(Type javaType, ThriftType thriftType, Method method) {
    Preconditions.checkNotNull(javaType, "javaType is null");
    Preconditions.checkNotNull(thriftType, "thriftType is null");
    Preconditions.checkNotNull(method, "method is null");
    Preconditions.checkArgument(!thriftType.isCoerced(), "thriftType is already coerced");

    this.javaType = javaType;
    this.thriftType = thriftType.coerceTo(javaType);
    this.method = method;
  }

  public Type getJavaType() {
    return javaType;
  }

  public ThriftType getThriftType() {
    return thriftType;
  }

  public Method getMethod() {
    return method;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("JavaToThriftCoercion");
    sb.append("{javaType=").append(javaType);
    sb.append(", thriftType=").append(thriftType);
    sb.append(", method=").append(method);
    sb.append('}');
    return sb.toString();
  }
}
