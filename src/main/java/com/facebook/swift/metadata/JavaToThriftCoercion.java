/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class JavaToThriftCoercion {
  private final Type javaType;
  private final ThriftType thriftType;
  private final Method method;

  public JavaToThriftCoercion(Type javaType, ThriftType thriftType, Method method) {
    this.javaType = javaType;
    this.thriftType = thriftType;
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
