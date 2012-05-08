/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.metadata;

import com.facebook.swift.codec.ThriftProtocolType;
import com.facebook.swift.codec.internal.coercion.DefaultJavaCoercions;
import com.facebook.swift.codec.internal.coercion.FromThrift;
import com.facebook.swift.codec.internal.coercion.ToThrift;
import com.facebook.swift.codec.metadata.Problems.Monitor;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.facebook.swift.codec.ThriftProtocolType.inferProtocolType;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getIterableType;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getMapKeyType;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getMapValueType;
import static com.facebook.swift.codec.metadata.ThriftType.BOOL;
import static com.facebook.swift.codec.metadata.ThriftType.BYTE;
import static com.facebook.swift.codec.metadata.ThriftType.DOUBLE;
import static com.facebook.swift.codec.metadata.ThriftType.I16;
import static com.facebook.swift.codec.metadata.ThriftType.I32;
import static com.facebook.swift.codec.metadata.ThriftType.I64;
import static com.facebook.swift.codec.metadata.ThriftType.STRING;
import static com.facebook.swift.codec.metadata.ThriftType.VOID;
import static com.facebook.swift.codec.metadata.ThriftType.enumType;
import static com.facebook.swift.codec.metadata.ThriftType.list;
import static com.facebook.swift.codec.metadata.ThriftType.map;
import static com.facebook.swift.codec.metadata.ThriftType.set;
import static com.facebook.swift.codec.metadata.ThriftType.struct;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

/**
 * ThriftCatalog contains the metadata for all known structs, enums and type coercions.  Since,
 * metadata extraction can be very expensive, and only single instance of the catalog should be
 * created.
 */
@ThreadSafe
public class ThriftCatalog {
  private final Problems.Monitor monitor;
  private final ConcurrentMap<Class<?>, ThriftStructMetadata<?>> structs =
      new ConcurrentHashMap<>();
  private final ConcurrentMap<Class<?>, ThriftEnumMetadata<?>> enums = new ConcurrentHashMap<>();
  private final ConcurrentMap<Type, TypeCoercion> coercions = new ConcurrentHashMap<>();

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

  @VisibleForTesting
  Monitor getMonitor() {
    return monitor;
  }

  /**
   * Add the @ToThrift and @FromThrift coercions in the specified class to this catalog.  All
   * coercions must be symmetrical, so ever @ToThrift method must have a corresponding @FromThrift
   * method.
   */
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

    // assure coercions are symmetric
    Set<ThriftType> difference = Sets.symmetricDifference(
        toThriftCoercions.keySet(),
        fromThriftCoercions.keySet()
    );
    Preconditions.checkArgument(
        difference.isEmpty(),
        "Coercion class %s does not have matched @ToThrift and @FromThrift methods for types %s",
        coercionsClass.getName(),
        difference
    );

    // add the coercions
    Map<Type, TypeCoercion> coercions = new HashMap<>();
    for (Map.Entry<ThriftType, Method> entry : toThriftCoercions.entrySet()) {
      ThriftType type = entry.getKey();
      Method toThriftMethod = entry.getValue();
      Method fromThriftMethod = fromThriftCoercions.get(type);
      Preconditions.checkState(fromThriftCoercions != null);
      TypeCoercion coercion = new TypeCoercion(type, toThriftMethod, fromThriftMethod);
      coercions.put(type.getJavaType(), coercion);
    }
    this.coercions.putAll(coercions);
  }

  /**
   * Gets the default ThriftCoercion for the specified type.
   */
  public TypeCoercion getDefaultCoercion(Type type) {
    return coercions.get(type);
  }

  /**
   * Gets the ThriftType for the specified Java type.  The native Thrift type for the Java type will
   * be inferred from the Java type, and if necessary type coercions will be applied.
   */
  public ThriftType getThriftType(Type javaType) {
    ThriftProtocolType protocolType = inferProtocolType(javaType);
    if (protocolType != null) {
      return getThriftType(javaType, protocolType);
    }

    // coerce the type if possible
    TypeCoercion coercion = coercions.get(javaType);
    if (coercion != null) {
      return coercion.getThriftType();
    }
    throw new RuntimeException(
        "Type is not annotated with @ThriftStruct or an automatically " +
            "supported type: " + javaType
    );
  }

  /**
   * Gets the ThriftType for the specified Java type encoded as the specified Thrift type.  This
   * method can create type that require coercions that have not been registered with this catalog.
   */
  public ThriftType getThriftType(Type javaType, ThriftProtocolType protocolType) {
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
        if (structClass == void.class) {
          return VOID;
        }
        ThriftStructMetadata<?> structMetadata = getThriftStructMetadata(structClass);
        return struct(structMetadata);
      }
      case MAP: {
        Type mapKeyType = getMapKeyType(javaType);
        Type mapValueType = getMapValueType(javaType);
        return map(getThriftType(mapKeyType), getThriftType(mapValueType));
      }
      case SET: {
        Type elementType = getIterableType(javaType);
        return set(getThriftType(elementType));
      }
      case LIST: {
        Type elementType = getIterableType(javaType);
        return list(getThriftType(elementType));
      }
      case ENUM: {
        Class<?> enumClass = TypeToken.of(javaType).getRawType();
        ThriftEnumMetadata<? extends Enum<?>> thriftEnumMetadata = getThriftEnumMetadata(enumClass);
        return enumType(thriftEnumMetadata);
      }
      default: {
        throw new IllegalStateException("Write does not support fields of type " + protocolType);
      }
    }
  }

  /**
   * Gets the ThriftEnumMetadata for the specified enum class.  If the enum class contains a method
   * annotated with @ThriftEnumValue, the value of this method will be used for the encoded thrift
   * value; otherwise the Enum.ordinal() method will be used.
   */
  public <T extends Enum<T>> ThriftEnumMetadata<?> getThriftEnumMetadata(Class<?> enumClass) {
    ThriftEnumMetadata<?> enumMetadata = enums.get(enumClass);
    if (enumMetadata == null) {
      enumMetadata = new ThriftEnumMetadata<>((Class<T>) enumClass);

      ThriftEnumMetadata<?> current = enums.putIfAbsent(enumClass, enumMetadata);
      if (current != null) {
        enumMetadata = current;
      }
    }
    return enumMetadata;
  }

  /**
   * Gets the ThriftStructMetadata for the specified struct class.  The struct class must be
   * annotated with @ThriftStruct.
   */
  public <T> ThriftStructMetadata<T> getThriftStructMetadata(Class<T> structClass) {
    ThriftStructMetadata<?> structMetadata = structs.get(structClass);
    if (structMetadata == null) {
      structMetadata = extractThriftStructMetadata(structClass);

      ThriftStructMetadata<?> current = structs.putIfAbsent(
          structClass,
          structMetadata
      );
      if (current != null) {
        structMetadata = current;
      }
    }
    return (ThriftStructMetadata<T>) structMetadata;
  }

  private <T> ThriftStructMetadata<T> extractThriftStructMetadata(Class<T> structClass) {
    Preconditions.checkNotNull(structClass, "structClass is null");

    Deque<Class<?>> stack = this.stack.get();
    if (stack.contains(structClass)) {
      String path = Joiner.on("->").join(
          transform(
              concat(stack, ImmutableList.of(structClass)), new Function<Class<?>, Object>() {
            @Override
            public Object apply(@Nullable Class<?> input) {
              return input.getName();
            }
          }
          )
      );
      throw new IllegalArgumentException("Circular references are not allowed: " + path);
    }

    stack.push(structClass);
    try {
        ThriftStructMetadataBuilder<T> builder = new ThriftStructMetadataBuilder<>(
            this,
            structClass
        );
      ThriftStructMetadata<T> structMetadata = builder.build();
      return structMetadata;
    } finally {
      Class<?> top = stack.pop();
      checkState(
          structClass.equals(top),
          "ThriftCatalog circularity detection stack is corrupt: expected %s, but got %s",
          structClass,
          top
      );
    }
  }
}
