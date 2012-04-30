/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.lang.reflect.Constructor;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThriftConstructorInjection {
  private final Constructor<?> constructor;
  private final List<ThriftParameterInjection> parameters;

  public ThriftConstructorInjection(
    Constructor<?> constructor,
    ThriftParameterInjection... parameters
  ) {
    this(
      checkNotNull(constructor, "constructor is null"),
      ImmutableList.copyOf(checkNotNull(parameters, "parameters is null"))
    );
  }

  public ThriftConstructorInjection(
    Constructor<?> constructor,
    List<ThriftParameterInjection> parameters
  ) {
    this.constructor = checkNotNull(constructor, "constructor is null");
    this.parameters = ImmutableList.copyOf(checkNotNull(parameters, "parameters is null"));
  }

  public Constructor<?> getConstructor() {
    return constructor;
  }

  public List<ThriftParameterInjection> getParameters() {
    return parameters;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(constructor.getName());
    sb.append('(');
    Joiner.on(", ").appendTo(sb, parameters);
    sb.append(')');
    return sb.toString();
  }
}
