/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import com.google.common.base.Preconditions;

import java.lang.reflect.Field;

public class ThriftFieldInjection implements ThriftInjection {
  private final short id;
  private final String name;
  private final Field field;
  private final TypeCoercion coercion;

  public ThriftFieldInjection(short id, String name, TypeCoercion coercion, Field field) {
    Preconditions.checkArgument(id >= 0, "fieldId is negative");
    Preconditions.checkNotNull(name, "name is null");
    Preconditions.checkNotNull(field, "field is null");

    this.id = id;
    this.name = name;
    this.coercion = coercion;
    this.field = field;
  }

  @Override
  public short getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public TypeCoercion getCoercion() {
    return coercion;
  }

  public Field getField() {
    return field;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ThriftFieldInjection");
    sb.append("{fieldId=").append(id);
    sb.append(", name=").append(name);
    sb.append(", coercion=").append(coercion);
    sb.append(", field=").append(field.getDeclaringClass().getSimpleName()).append(".").append(
      field.getName()
    );
    sb.append('}');
    return sb.toString();
  }
}
