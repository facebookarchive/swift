/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ThriftFieldMetadata {
  private final short id;
  private final ThriftType type;
  private final String name;
  private final List<ThriftInjection> injections;
  private final ThriftExtraction extraction;
  private final TypeCoercion coercion;

  public ThriftFieldMetadata(
      short id,
      ThriftType type,
      String name,
      List<ThriftInjection> injections,
      ThriftExtraction extraction,
      TypeCoercion coercion
  ) {
    checkArgument(id >= 0, "id is negative");
    checkNotNull(type, "type is null");
    checkNotNull(name, "name is null");
    checkNotNull(injections, "injections is null");
    checkArgument(
        !injections.isEmpty() || extraction != null,
        "A thrift field must have an injection or extraction point"
    );

    this.id = id;
    this.type = type;
    this.name = name;
    this.injections = ImmutableList.copyOf(injections);
    this.extraction = extraction;
    this.coercion = coercion;
  }

  public short getId() {
    return id;
  }

  public ThriftType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public boolean isReadable() {
    return extraction != null;
  }

  public boolean isWritable() {
    return !injections.isEmpty();
  }

  public boolean isReadOnly() {
    return injections.isEmpty();
  }

  public boolean isWriteOnly() {
    return extraction == null;
  }

  public List<ThriftInjection> getInjections() {
    return injections;
  }

  public ThriftExtraction getExtraction() {
    return extraction;
  }

  public TypeCoercion getCoercion() {
    return coercion;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ThriftFieldMetadata");
    sb.append("{id=").append(id);
    sb.append(", type=").append(type);
    sb.append(", name='").append(name).append('\'');
    sb.append(", injections=").append(injections);
    sb.append(", extraction=").append(extraction);
    sb.append(", coercion=").append(coercion);
    sb.append('}');
    return sb.toString();
  }
}
