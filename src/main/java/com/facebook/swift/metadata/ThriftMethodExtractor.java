/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ThriftMethodExtractor implements ThriftExtraction {
  private final short id;
  private final String name;
  private final Method method;

  public ThriftMethodExtractor(short id, String name, Method method) {
    checkArgument(id >= 0, "fieldId is negative");
    checkNotNull(name, "name is null");
    checkNotNull(method, "method is null");

    this.id = id;
    this.name = name;
    this.method = method;
  }

  @Override
  public short getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  public Method getMethod() {
    return method;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ThriftMethodExtractor");
    sb.append("{id=").append(id);
    sb.append(", name='").append(name).append('\'');
    sb.append(", method=").append(method);
    sb.append('}');
    return sb.toString();
  }
}
