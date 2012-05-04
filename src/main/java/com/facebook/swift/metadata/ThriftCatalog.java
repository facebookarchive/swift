/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import com.facebook.swift.ThriftProtocolFieldType;
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
  private final Map<CoercionKey, JavaToThriftCoercion> javaToThriftCoercions = new HashMap<>();
  private final Map<CoercionKey, ThriftToJavaCoercion> thriftToJavaCoercions = new HashMap<>();

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
  }

  Monitor getMonitor() {
    return monitor;
  }

  public void addGeneralCoercions(Class<?> coercionsClass) {
    Map<CoercionKey, JavaToThriftCoercion> toThriftCoercions = new HashMap<>();
    Map<CoercionKey, ThriftToJavaCoercion> fromThriftCoercions = new HashMap<>();
    for (Method method : coercionsClass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(ToThrift.class)) {
        Preconditions.checkArgument(Modifier.isStatic(method.getModifiers()),
            "Method %s is not static", method.toGenericString()
        );
        Type javaType = method.getGenericParameterTypes()[0];
        ThriftType thriftType = getThriftType(method.getGenericReturnType());
        Preconditions.checkArgument(thriftType != null,
            "Method %s does not return a known thrift type", method.toGenericString()
        );
        JavaToThriftCoercion coercion = new JavaToThriftCoercion(
            javaType,
            thriftType,
            method
        );
        toThriftCoercions.put(new CoercionKey(javaType, null), coercion);
        toThriftCoercions.put(new CoercionKey(javaType, thriftType.getProtocolType()), coercion);
      } else if (method.isAnnotationPresent(FromThrift.class)) {
        Preconditions.checkArgument(Modifier.isStatic(method.getModifiers()),
            "Method %s is not static", method.toGenericString()
        );
        Type javaType = method.getGenericReturnType();
        ThriftType thriftType = getThriftType( method.getGenericParameterTypes()[0]);
        Preconditions.checkArgument(thriftType != null,
            "Method %s does not return a known thrift type", method.toGenericString()
        );
        ThriftToJavaCoercion coercion = new ThriftToJavaCoercion(
            thriftType,
            javaType,
            method
        );
        fromThriftCoercions.put(new CoercionKey(javaType, null), coercion);
        fromThriftCoercions.put(new CoercionKey(javaType, thriftType.getProtocolType()), coercion);
      }
    }
    javaToThriftCoercions.putAll(toThriftCoercions);
    thriftToJavaCoercions.putAll(fromThriftCoercions);
  }

  public void addGeneralCoercion(JavaToThriftCoercion coercion) {
    javaToThriftCoercions.put(new CoercionKey(coercion.getJavaType(), null), coercion);
    addSpecificCoercion(coercion);
  }

  public void addGeneralCoercion(ThriftToJavaCoercion coercion) {
    thriftToJavaCoercions.put(new CoercionKey(coercion.getJavaType(), null), coercion);
    addSpecificCoercion(coercion);
  }

  public void addSpecificCoercion(JavaToThriftCoercion coercion) {
    javaToThriftCoercions.put(
        new CoercionKey(coercion.getJavaType(), coercion.getThriftType().getProtocolType()),
        coercion
    );
  }

  public void addSpecificCoercion(ThriftToJavaCoercion coercion) {
    thriftToJavaCoercions.put(
        new CoercionKey(coercion.getJavaType(), coercion.getThriftType().getProtocolType()),
        coercion
    );
  }

  public JavaToThriftCoercion getJavaToThriftCoercion(
      Type javaType,
      ThriftProtocolFieldType thriftType) {
    return javaToThriftCoercions.get(new CoercionKey(javaType, thriftType));
  }

  public ThriftToJavaCoercion getThriftToJavaCoercion(
      Type javaType,
      ThriftProtocolFieldType thriftType) {
    return thriftToJavaCoercions.get(new CoercionKey(javaType, thriftType));
  }

  public ThriftType getThriftType(Type javaType) {
    ThriftProtocolFieldType protocolType = inferProtocolType(javaType);
    if (protocolType != null) {
      return getThriftType(javaType, protocolType);
    }

    // coerce the type if possible
    JavaToThriftCoercion toThrift = javaToThriftCoercions.get(new CoercionKey(javaType, null));
    if (toThrift != null) {
      return toThrift.getThriftType();
    }
    ThriftToJavaCoercion fromThrift = thriftToJavaCoercions.get(new CoercionKey(javaType, null));
    if (fromThrift != null) {
      return fromThrift.getThriftType();
    }
    throw new RuntimeException("Unsupported java type: " + javaType);
  }

  public ThriftType getThriftType(Type javaType, ThriftProtocolFieldType protocolType) {
    switch (protocolType) {
      case BOOL:
        return BOOL;
      case BYTE:
        return BYTE;
      case DOUBLE:
        return DOUBLE;
      case I16:
        return I16;
      case I32:
        return I32;
      case I64:
        return I64;
      case STRING:
        return STRING;
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

  private static class CoercionKey {
    private final Type javaType;
    private final ThriftProtocolFieldType thriftType;

    private CoercionKey(Type javaType, ThriftProtocolFieldType thriftType) {
      Preconditions.checkNotNull(javaType, "javaType is null");
      this.javaType = javaType;
      this.thriftType = thriftType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final CoercionKey that = (CoercionKey) o;

      if (!javaType.equals(that.javaType)) {
        return false;
      }
      if (thriftType != that.thriftType) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = javaType.hashCode();
      result = 31 * result + (thriftType != null ? thriftType.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("CoercionKey");
      sb.append("{javaType=").append(javaType);
      sb.append(", thriftType=").append(thriftType);
      sb.append('}');
      return sb.toString();
    }
  }
}
