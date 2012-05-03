/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class ThriftToJavaCoercion {
  private final ThriftType thriftType;
  private final Type javaType;
  private final Method method;

  public ThriftToJavaCoercion(ThriftType thriftType, Type javaType, Method method) {
    this.thriftType = thriftType;
    this.javaType = javaType;
    this.method = method;
  }

  public ThriftType getThriftType() {
    return thriftType;
  }

  public Type getJavaType() {
    return javaType;
  }

  public Method getMethod() {
    return method;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ThriftToJavaCoercion");
    sb.append("{thriftType=").append(thriftType);
    sb.append(", javaType=").append(javaType);
    sb.append(", method=").append(method);
    sb.append('}');
    return sb.toString();
  }
}
