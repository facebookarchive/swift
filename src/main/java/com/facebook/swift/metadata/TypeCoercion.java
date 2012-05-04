/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class TypeCoercion {
  private final Type javaType;
  private final ThriftType thriftType;
  private final Method toThrift;
  private final Method fromThrift;

  public TypeCoercion(Type javaType, ThriftType thriftType, Method toThrift, Method fromThrift) {
    this.javaType = javaType;
    this.thriftType = thriftType;
    this.toThrift = toThrift;
    this.fromThrift = fromThrift;
  }

  public Type getJavaType() {
    return javaType;
  }

  public ThriftType getThriftType() {
    return thriftType;
  }

  public Method getToThrift() {
    return toThrift;
  }

  public Method getFromThrift() {
    return fromThrift;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("TypeCoercion");
    sb.append("{javaType=").append(javaType);
    sb.append(", thriftType=").append(thriftType);
    sb.append(", toThrift=").append(toThrift);
    sb.append(", fromThrift=").append(fromThrift);
    sb.append('}');
    return sb.toString();
  }
}
