/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import com.facebook.swift.ThriftProtocolFieldType;
import com.facebook.swift.coercion.DefaultJavaCoercions;
import com.facebook.swift.coercion.FromThrift;
import com.facebook.swift.coercion.ToThrift;
import com.facebook.swift.metadata.Problems.Monitor;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.facebook.swift.ThriftProtocolFieldType.inferProtocolType;
import static com.facebook.swift.metadata.ThriftType.BOOL;
import static com.facebook.swift.metadata.ThriftType.BYTE;
import static com.facebook.swift.metadata.ThriftType.DOUBLE;
import static com.facebook.swift.metadata.ThriftType.I16;
import static com.facebook.swift.metadata.ThriftType.I32;
import static com.facebook.swift.metadata.ThriftType.I64;
import static com.facebook.swift.metadata.ThriftType.STRING;
import static com.facebook.swift.metadata.ThriftType.list;
import static com.facebook.swift.metadata.ThriftType.map;
import static com.facebook.swift.metadata.ThriftType.set;
import static com.facebook.swift.metadata.ThriftType.struct;
import static com.facebook.swift.metadata.TypeParameterUtils.getTypeParameters;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

public class ThriftCatalog {
  private final Problems.Monitor monitor;
  private final Map<Class<?>, ThriftStructMetadata<?>> structs = new HashMap<>();
  private final Map<Type, TypeCoercion> coercions = new HashMap<>();

  private final ThreadLocal<Deque<Class<?>>> stack = new ThreadLocal<Deque<Class<?>>>() {
    @Override
    protected Deque<Class<?>> initialValue() {
      return new ArrayDeque<>();
    }
  };

  public ThriftCatalog() {
    this(Problems.NULL_MONITOR);
  }

  @VisibleForTesting
  public ThriftCatalog(Monitor monitor) {
    this.monitor = monitor;
    addDefaultCoercions(DefaultJavaCoercions.class);
  }

  Monitor getMonitor() {
    return monitor;
  }

  public void addDefaultCoercions(Class<?> coercionsClass) {
    Map<ThriftType, Method> toThriftCoercions = new HashMap<>();
    Map<ThriftType, Method> fromThriftCoercions = new HashMap<>();
    for (Method method : coercionsClass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(ToThrift.class)) {
        Preconditions.checkArgument(
            Modifier.isStatic(method.getModifiers()),
            "Method %s is not static", method.toGenericString()
        );
        ThriftType thriftType = getThriftType(method.getGenericReturnType());
        Preconditions.checkArgument(
            thriftType != null,
            "Method %s does not return a known thrift type", method.toGenericString()
        );
        toThriftCoercions.put(thriftType.coerceTo(method.getGenericParameterTypes()[0]), method);
      } else if (method.isAnnotationPresent(FromThrift.class)) {
        Preconditions.checkArgument(
            Modifier.isStatic(method.getModifiers()),
            "Method %s is not static", method.toGenericString()
        );
        ThriftType thriftType = getThriftType(method.getGenericParameterTypes()[0]);
        Preconditions.checkArgument(
            thriftType != null,
            "Method %s does not return a known thrift type", method.toGenericString()
        );
        fromThriftCoercions.put(thriftType.coerceTo(method.getGenericReturnType()), method);
      }
    }
    Map<Type, TypeCoercion> coercions = new HashMap<>();
    for (Map.Entry<ThriftType, Method> entry : toThriftCoercions.entrySet()) {
      ThriftType type = entry.getKey();
      Method toThriftMethod = entry.getValue();
      Method fromThriftMethod = fromThriftCoercions.get(type);
      TypeCoercion coercion = new TypeCoercion(type, toThriftMethod, fromThriftMethod);
      coercions.put(type.getJavaType(), coercion);
//
//  TODO verify
//
    }
    this.coercions.putAll(coercions);
  }

  public TypeCoercion getDefaultCoercion(Type type) {
    return coercions.get(type);
  }

  public ThriftType getThriftType(Type javaType) {
    ThriftProtocolFieldType protocolType = inferProtocolType(javaType);
    if (protocolType != null) {
      return getThriftType(javaType, protocolType);
    }

    // coerce the type if possible
    TypeCoercion coercion = coercions.get(javaType);
    if (coercion != null) {
      return coercion.getThriftType();
    }
    throw new RuntimeException("Type is not annotated with @ThriftStruct or an automatically " +
        "supported type: " + javaType);
  }

  public ThriftType getThriftType(Type javaType, ThriftProtocolFieldType protocolType) {
    switch (protocolType) {
      case BOOL:
        return BOOL.coerceTo(javaType);
      case BYTE:
        return BYTE.coerceTo(javaType);
      case DOUBLE:
        return DOUBLE.coerceTo(javaType);
      case I16:
        return I16.coerceTo(javaType);
      case I32:
        return I32.coerceTo(javaType);
      case I64:
        return I64.coerceTo(javaType);
      case STRING:
        return STRING.coerceTo(javaType);
      case STRUCT: {
        Class<?> structClass = (Class<?>) javaType;
        ThriftStructMetadata<?> structMetadata = getThriftStructMetadata(structClass);
        return struct(structMetadata);
      }
      case MAP: {
        Type[] types = getTypeParameters(Map.class, javaType);
        checkArgument(
          types != null && types.length == 2,
          "Unable to extract Map key and value types from %s",
          javaType
        );
        return map(getThriftType(types[0]), getThriftType(types[1]));
      }
      case SET: {
        Type[] types = getTypeParameters(Set.class, javaType);
        checkArgument(
          types != null && types.length == 1,
          "Unable to extract Set element type from %s",
          javaType
        );
        return set(getThriftType(types[0]));
      }
      case LIST: {
        Type[] types = getTypeParameters(Iterable.class, javaType);
        checkArgument(
          types != null && types.length == 1,
          "Unable to extract List element type from %s",
          javaType
        );
        return list(getThriftType(types[0]));
      }
      case ENUM: {
        // todo implement enums
        throw new UnsupportedOperationException("enums are not implemented");
      }
      default: {
        throw new IllegalStateException("Write does not support fields of type " + protocolType);
      }
    }
  }

  public <T> ThriftStructMetadata<T> getThriftStructMetadata(Class<T> configClass) {
    Preconditions.checkNotNull(configClass, "configClass is null");

    Deque<Class<?>> stack = this.stack.get();
    if (stack.contains(configClass)) {
      String path = Joiner.on("->").join(
        transform(
          concat(stack, ImmutableList.of(configClass)), new Function<Class<?>, Object>() {
          @Override
          public Object apply(@Nullable Class<?> input) {
            return input.getName();
          }
        }
        )
      );
      throw new IllegalArgumentException("Circular references are not allowed: " + path);
    }

    stack.push(configClass);
    try {
      ThriftStructMetadata<T> structMetadata = (ThriftStructMetadata<T>) structs.get(configClass);
      if (structMetadata == null) {
        ThriftStructMetadataBuilder<T> builder = new ThriftStructMetadataBuilder<>(
          this,
          configClass
        );
        structMetadata = builder.build();
        structs.put(configClass, structMetadata);
      }
      return structMetadata;
    } finally {
      Class<?> top = stack.pop();
      checkState(
        configClass.equals(top),
        "ThriftCatalog circularity detection stack is corrupt: expected %s, but got %s",
        configClass,
        top
      );
    }
  }
}
