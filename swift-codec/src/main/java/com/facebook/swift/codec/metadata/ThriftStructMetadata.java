/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.metadata;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThriftStructMetadata<T> {
  private final String structName;
  private final Class<T> structClass;

  private final Class<?> builderClass;
  private final ThriftMethodInjection builderMethod;

  private final SortedMap<Short, ThriftFieldMetadata> fields;

  private final ThriftConstructorInjection constructor;
  private final List<ThriftMethodInjection> methodInjections;

  public ThriftStructMetadata(
    String structName,
    Class<T> structClass,
    Class<?> builderClass,
    ThriftMethodInjection builderMethod,
    List<ThriftFieldMetadata> fields,
    ThriftConstructorInjection constructor,
    List<ThriftMethodInjection> methodInjections
  ) {
    this.builderClass = builderClass;
    this.builderMethod = builderMethod;
    this.structName = checkNotNull(structName, "structName is null");
    this.structClass = checkNotNull(structClass, "structClass is null");
    this.constructor = checkNotNull(constructor, "constructor is null");
    this.fields = ImmutableSortedMap.copyOf(Maps.uniqueIndex(
      checkNotNull(fields, "fields is null"), new Function<ThriftFieldMetadata, Short>() {
      @Override
      public Short apply(@Nullable ThriftFieldMetadata input) {
        return input.getId();
      }
    }
    ));
    this.methodInjections = ImmutableList.copyOf(
      checkNotNull(
        methodInjections,
        "methodInjections is null"
      )
    );
  }

  public String getStructName() {
    return structName;
  }

  public Class<T> getStructClass() {
    return structClass;
  }

  public Class<?> getBuilderClass() {
    return builderClass;
  }

  public ThriftMethodInjection getBuilderMethod() {
    return builderMethod;
  }

  public ThriftFieldMetadata getField(int id) {
    return fields.get((short) id);
  }

  public Collection<ThriftFieldMetadata> getFields() {
    return fields.values();
  }

  public ThriftConstructorInjection getConstructor() {
    return constructor;
  }

  public List<ThriftMethodInjection> getMethodInjections() {
    return methodInjections;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ThriftStructMetadata");
    sb.append("{structName='").append(structName).append('\'');
    sb.append(", structClass=").append(structClass);
    sb.append(", builderClass=").append(builderClass);
    sb.append(", builderMethod=").append(builderMethod);
    sb.append(", fields=").append(fields);
    sb.append(", constructor=").append(constructor);
    sb.append(", methodInjections=").append(methodInjections);
    sb.append('}');
    return sb.toString();
  }
}
