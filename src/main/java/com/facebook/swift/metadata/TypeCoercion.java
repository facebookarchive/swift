/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import java.lang.reflect.Method;

public class TypeCoercion {
  private final ThriftType thriftType;
  private final Method toThrift;
  private final Method fromThrift;

  public TypeCoercion(ThriftType thriftType, Method toThrift, Method fromThrift) {
    this.thriftType = thriftType;
    this.toThrift = toThrift;
    this.fromThrift = fromThrift;
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
    sb.append("{thriftType=").append(thriftType);
    sb.append(", toThrift=").append(toThrift);
    sb.append(", fromThrift=").append(fromThrift);
    sb.append('}');
    return sb.toString();
  }
}
