/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.codec.metadata;

import com.facebook.swift.codec.ThriftDocumentation;
import com.facebook.swift.codec.ThriftOrder;
import com.facebook.swift.codec.ThriftStruct;
import com.facebook.swift.codec.ThriftUnion;
import com.facebook.swift.codec.internal.coercion.DefaultJavaCoercions;
import com.facebook.swift.codec.internal.coercion.FromThrift;
import com.facebook.swift.codec.internal.coercion.ToThrift;
import com.facebook.swift.codec.metadata.MetadataErrors.Monitor;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.concurrent.ThreadSafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.facebook.swift.codec.metadata.ReflectionHelper.getFutureReturnType;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getIterableType;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getMapKeyType;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getMapValueType;
import static com.facebook.swift.codec.metadata.ThriftType.BINARY;
import static com.facebook.swift.codec.metadata.ThriftType.BOOL;
import static com.facebook.swift.codec.metadata.ThriftType.BYTE;
import static com.facebook.swift.codec.metadata.ThriftType.DOUBLE;
import static com.facebook.swift.codec.metadata.ThriftType.I16;
import static com.facebook.swift.codec.metadata.ThriftType.I32;
import static com.facebook.swift.codec.metadata.ThriftType.I64;
import static com.facebook.swift.codec.metadata.ThriftType.STRING;
import static com.facebook.swift.codec.metadata.ThriftType.VOID;
import static com.facebook.swift.codec.metadata.ThriftType.array;
import static com.facebook.swift.codec.metadata.ThriftType.enumType;
import static com.facebook.swift.codec.metadata.ThriftType.list;
import static com.facebook.swift.codec.metadata.ThriftType.map;
import static com.facebook.swift.codec.metadata.ThriftType.set;
import static com.facebook.swift.codec.metadata.ThriftType.struct;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

import static java.lang.reflect.Modifier.isStatic;

/**
 * ThriftCatalog contains the metadata for all known structs, enums and type coercions.  Since,
 * metadata extraction can be very expensive, and only single instance of the catalog should be
 * created.
 */
@ThreadSafe
public class ThriftCatalog
{
    private final MetadataErrors.Monitor monitor;
    private final ConcurrentMap<Type, ThriftStructMetadata> structs = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, ThriftEnumMetadata<?>> enums = new ConcurrentHashMap<>();
    private final ConcurrentMap<Type, TypeCoercion> coercions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, ThriftType> manualTypes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Type, ThriftType> typeCache = new ConcurrentHashMap<>();

    private final ThreadLocal<Deque<Type>> stack = new ThreadLocal<Deque<Type>>()
    {
        @Override
        protected Deque<Type> initialValue()
        {
            return new ArrayDeque<>();
        }
    };

    public ThriftCatalog()
    {
        this(MetadataErrors.NULL_MONITOR);
    }

    @VisibleForTesting
    public ThriftCatalog(Monitor monitor)
    {
        this.monitor = monitor;
        addDefaultCoercions(DefaultJavaCoercions.class);
    }

    @VisibleForTesting
    Monitor getMonitor()
    {
        return monitor;
    }

    public void addThriftType(ThriftType thriftType)
    {
        manualTypes.put(TypeToken.of(thriftType.getJavaType()).getRawType(), thriftType);
    }

    /**
     * Add the @ToThrift and @FromThrift coercions in the specified class to this catalog.
     * All coercions must be symmetrical, so every @ToThrift method must have a
     * corresponding @FromThrift method.
     */
    public void addDefaultCoercions(Class<?> coercionsClass)
    {
        Preconditions.checkNotNull(coercionsClass, "coercionsClass is null");
        Map<ThriftType, Method> toThriftCoercions = new HashMap<>();
        Map<ThriftType, Method> fromThriftCoercions = new HashMap<>();
        for (Method method : coercionsClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ToThrift.class)) {
                verifyCoercionMethod(method);
                ThriftType thriftType = getThriftType(method.getGenericReturnType());
                ThriftType coercedType = thriftType.coerceTo(method.getGenericParameterTypes()[0]);

                Method oldValue = toThriftCoercions.put(coercedType, method);
                Preconditions.checkArgument(
                        oldValue == null,
                        "Coercion class two @ToThrift methods (%s and %s) for type %s",
                        coercionsClass.getName(),
                        method,
                        oldValue,
                        coercedType);
            }
            else if (method.isAnnotationPresent(FromThrift.class)) {
                verifyCoercionMethod(method);
                ThriftType thriftType = getThriftType(method.getGenericParameterTypes()[0]);
                ThriftType coercedType = thriftType.coerceTo(method.getGenericReturnType());

                Method oldValue = fromThriftCoercions.put(coercedType, method);
                Preconditions.checkArgument(
                        oldValue == null,
                        "Coercion class two @FromThrift methods (%s and %s) for type %s",
                        coercionsClass.getName(),
                        method,
                        oldValue,
                        coercedType);
            }
        }

        // assure coercions are symmetric
        Set<ThriftType> difference = Sets.symmetricDifference(toThriftCoercions.keySet(), fromThriftCoercions.keySet());
        Preconditions.checkArgument(
                difference.isEmpty(),
                "Coercion class %s does not have matched @ToThrift and @FromThrift methods for types %s",
                coercionsClass.getName(),
                difference);

        // add the coercions
        Map<Type, TypeCoercion> coercions = new HashMap<>();
        for (Map.Entry<ThriftType, Method> entry : toThriftCoercions.entrySet()) {
            ThriftType type = entry.getKey();
            Method toThriftMethod = entry.getValue();
            Method fromThriftMethod = fromThriftCoercions.get(type);
            // this should never happen due to the difference check above, but be careful
            Preconditions.checkState(
                    fromThriftMethod != null,
                    "Coercion class %s does not have matched @ToThrift and @FromThrift methods for type %s",
                    coercionsClass.getName(),
                    type);
            TypeCoercion coercion = new TypeCoercion(type, toThriftMethod, fromThriftMethod);
            coercions.put(type.getJavaType(), coercion);
        }
        this.coercions.putAll(coercions);
    }

    private void verifyCoercionMethod(Method method)
    {
        Preconditions.checkArgument(isStatic(method.getModifiers()), "Method %s is not static", method.toGenericString());
        Preconditions.checkArgument(method.getParameterTypes().length == 1, "Method %s must have exactly one parameter", method.toGenericString());
        Preconditions.checkArgument(method.getReturnType() != void.class, "Method %s must have a return value", method.toGenericString());
    }

    /**
     * Gets the default TypeCoercion (and associated ThriftType) for the specified Java type.
     */
    public TypeCoercion getDefaultCoercion(Type type)
    {
        return coercions.get(type);
    }

    /**
     * Gets the ThriftType for the specified Java type.  The native Thrift type for the Java type will
     * be inferred from the Java type, and if necessary type coercions will be applied.
     *
     * @return the ThriftType for the specified java type; never null
     * @throws IllegalArgumentException if the Java Type can not be coerced to a ThriftType
     */
    public ThriftType getThriftType(Type javaType)
            throws IllegalArgumentException
    {
        ThriftType thriftType = typeCache.get(javaType);
        if (thriftType == null) {
            thriftType = getThriftTypeUncached(javaType);
            typeCache.putIfAbsent(javaType, thriftType);
        }
        return thriftType;
    }

    private ThriftType getThriftTypeUncached(Type javaType)
            throws IllegalArgumentException
    {
        Class<?> rawType = TypeToken.of(javaType).getRawType();
        ThriftType manualType = manualTypes.get(rawType);
        if (manualType != null) {
            return manualType;
        }
        if (boolean.class == rawType) {
            return BOOL;
        }
        if (byte.class == rawType) {
            return BYTE;
        }
        if (short.class == rawType) {
            return I16;
        }
        if (int.class == rawType) {
            return I32;
        }
        if (long.class == rawType) {
            return I64;
        }
        if (double.class == rawType) {
            return DOUBLE;
        }
        if (String.class == rawType) {
            return STRING;
        }
        if (ByteBuffer.class.isAssignableFrom(rawType)) {
            return BINARY;
        }
        if (Enum.class.isAssignableFrom(rawType)) {
            ThriftEnumMetadata<? extends Enum<?>> thriftEnumMetadata = getThriftEnumMetadata(rawType);
            return enumType(thriftEnumMetadata);
        }
        if (rawType.isArray()) {
            Class<?> elementType = rawType.getComponentType();
            if (elementType == byte.class) {
                // byte[] is encoded as BINARY and requires a coersion
                return coercions.get(javaType).getThriftType();
            }
            return array(getThriftType(elementType));
        }
        if (Map.class.isAssignableFrom(rawType)) {
            Type mapKeyType = getMapKeyType(javaType);
            Type mapValueType = getMapValueType(javaType);
            return map(getThriftType(mapKeyType), getThriftType(mapValueType));
        }
        if (Set.class.isAssignableFrom(rawType)) {
            Type elementType = getIterableType(javaType);
            return set(getThriftType(elementType));
        }
        if (Iterable.class.isAssignableFrom(rawType)) {
            Type elementType = getIterableType(javaType);
            return list(getThriftType(elementType));
        }
        // The void type is used by service methods and is encoded as an empty struct
        if (void.class.isAssignableFrom(rawType) || Void.class.isAssignableFrom(rawType)) {
            return VOID;
        }
        if (rawType.isAnnotationPresent(ThriftStruct.class)) {
            ThriftStructMetadata structMetadata = getThriftStructMetadata(javaType);
            return struct(structMetadata);
        }
        if (rawType.isAnnotationPresent(ThriftUnion.class)) {
            ThriftStructMetadata structMetadata = getThriftStructMetadata(javaType);
            // An union looks like a struct with a single field.
            return struct(structMetadata);
        }

        if (ListenableFuture.class.isAssignableFrom(rawType)) {
            Type returnType = getFutureReturnType(javaType);
            // TODO: check that we aren't recursing through multiple futures
            // TODO: find a way to restrict this to return values only
            return getThriftType(returnType);
        }

        // coerce the type if possible
        TypeCoercion coercion = coercions.get(javaType);
        if (coercion != null) {
            return coercion.getThriftType();
        }
        throw new IllegalArgumentException("Type can not be coerced to a Thrift type: " + javaType);
    }

    public boolean isSupportedStructFieldType(Type javaType)
    {
        Class<?> rawType = TypeToken.of(javaType).getRawType();
        if (boolean.class == rawType) {
            return true;
        }
        if (byte.class == rawType) {
            return true;
        }
        if (short.class == rawType) {
            return true;
        }
        if (int.class == rawType) {
            return true;
        }
        if (long.class == rawType) {
            return true;
        }
        if (double.class == rawType) {
            return true;
        }
        if (String.class == rawType) {
            return true;
        }
        if (ByteBuffer.class.isAssignableFrom(rawType)) {
            return true;
        }
        if (Enum.class.isAssignableFrom(rawType)) {
            return true;
        }
        if (rawType.isArray()) {
            Class<?> elementType = rawType.getComponentType();
            return isSupportedArrayComponentType(elementType);
        }
        if (Map.class.isAssignableFrom(rawType)) {
            Type mapKeyType = getMapKeyType(javaType);
            Type mapValueType = getMapValueType(javaType);
            return isSupportedStructFieldType(mapKeyType) && isSupportedStructFieldType(mapValueType);
        }
        if (Set.class.isAssignableFrom(rawType)) {
            Type elementType = getIterableType(javaType);
            return isSupportedStructFieldType(elementType);
        }
        if (Iterable.class.isAssignableFrom(rawType)) {
            Type elementType = getIterableType(javaType);
            return isSupportedStructFieldType(elementType);
        }
        if (rawType.isAnnotationPresent(ThriftStruct.class)) {
            return true;
        }
        if (rawType.isAnnotationPresent(ThriftUnion.class)) {
            return true;
        }

        // NOTE: void is not a supported struct type

        // coerce the type if possible
        TypeCoercion coercion = coercions.get(javaType);
        if (coercion != null) {
            return true;
        }
        return false;
    }

    public boolean isSupportedArrayComponentType(Class<?> componentType)
    {
        return boolean.class == componentType ||
                byte.class == componentType ||
                short.class == componentType ||
                int.class == componentType ||
                long.class == componentType ||
                double.class == componentType;
    }

    /**
     * Gets the ThriftEnumMetadata for the specified enum class.  If the enum class contains a method
     * annotated with @ThriftEnumValue, the value of this method will be used for the encoded thrift
     * value; otherwise the Enum.ordinal() method will be used.
     */
    public <T extends Enum<T>> ThriftEnumMetadata<?> getThriftEnumMetadata(Class<?> enumClass)
    {
        ThriftEnumMetadata<?> enumMetadata = enums.get(enumClass);
        if (enumMetadata == null) {
            enumMetadata = new ThriftEnumMetadataBuilder<>((Class<T>) enumClass).build();

            ThriftEnumMetadata<?> current = enums.putIfAbsent(enumClass, enumMetadata);
            if (current != null) {
                enumMetadata = current;
            }
        }
        return enumMetadata;
    }

    /**
     * Gets the ThriftStructMetadata for the specified struct class.  The struct class must be
     * annotated with @ThriftStruct or @ThriftUnion.
     */
    public <T> ThriftStructMetadata getThriftStructMetadata(Type structType)
    {
        ThriftStructMetadata structMetadata = structs.get(structType);
        Class<?> structClass = TypeToken.of(structType).getRawType();
        if (structMetadata == null) {
            if (structClass.isAnnotationPresent(ThriftStruct.class)) {
                structMetadata = extractThriftStructMetadata(structType);
            }
            else if (structClass.isAnnotationPresent(ThriftUnion.class)) {
                structMetadata = extractThriftUnionMetadata(structType);
            }
            else {
                throw new IllegalStateException("getThriftStructMetadata called on a class that has no @ThriftStruct or @ThriftUnion annotation");
            }

            ThriftStructMetadata current = structs.putIfAbsent(structType, structMetadata);
            if (current != null) {
                structMetadata = current;
            }
        }
        return structMetadata;
    }


    private static Class<?> getSwiftMetaClassOf(Class<?> cls) throws ClassNotFoundException
    {
        ClassLoader loader = cls.getClassLoader();
        if (loader == null) {
            throw new ClassNotFoundException("null class loader");
        }
        return loader.loadClass(cls.getName() + "$swift_meta");
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static ImmutableList<String> getThriftDocumentation(Class<?> objectClass)
    {
        ThriftDocumentation documentation = objectClass.getAnnotation(ThriftDocumentation.class);

        if (documentation == null) {
            try {
                Class<?> swiftDocsClass = getSwiftMetaClassOf(objectClass);

                documentation = swiftDocsClass.getAnnotation(ThriftDocumentation.class);
            }
            catch (ClassNotFoundException e) {
                // ignored
            }
        }

        return documentation == null ? ImmutableList.<String>of() : ImmutableList.copyOf(documentation.value());
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static ImmutableList<String> getThriftDocumentation(Method method)
    {
        ThriftDocumentation documentation = method.getAnnotation(ThriftDocumentation.class);

        if (documentation == null) {
            try {
                Class<?> swiftDocsClass = getSwiftMetaClassOf(method.getDeclaringClass());

                documentation = swiftDocsClass.getDeclaredMethod(method.getName()).getAnnotation(ThriftDocumentation.class);
            }
            catch (ReflectiveOperationException e) {
                // ignored
            }
        }

        return documentation == null ? ImmutableList.<String>of() : ImmutableList.copyOf(documentation.value());
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static ImmutableList<String> getThriftDocumentation(Field field)
    {
        ThriftDocumentation documentation = field.getAnnotation(ThriftDocumentation.class);

        if (documentation == null) {
            try {
                Class<?> swiftDocsClass = getSwiftMetaClassOf(field.getDeclaringClass());

                documentation = swiftDocsClass.getDeclaredField(field.getName()).getAnnotation(ThriftDocumentation.class);
            }
            catch (ReflectiveOperationException e) {
                // ignored
            }
        }

        return documentation == null ? ImmutableList.<String>of() : ImmutableList.copyOf(documentation.value());
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static <T extends Enum<T>> ImmutableList<String> getThriftDocumentation(Enum<T> enumConstant)
    {
        try {
            Field f = enumConstant.getDeclaringClass().getField(enumConstant.name());
            return getThriftDocumentation(f);
        } catch (ReflectiveOperationException e) {
            // ignore
        }
        return ImmutableList.<String>of();
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static Integer getMethodOrder(Method method)
    {
        ThriftOrder order = method.getAnnotation(ThriftOrder.class);

        if (order == null) {
            try {
                Class<?> swiftDocsClass = getSwiftMetaClassOf(method.getDeclaringClass());

                order = swiftDocsClass.getDeclaredMethod(method.getName()).getAnnotation(ThriftOrder.class);
            }
            catch (ReflectiveOperationException e) {
                // ignored
            }
        }

        return order == null ? null : order.value();
    }

    private ThriftStructMetadata extractThriftStructMetadata(Type structType)
    {
        Preconditions.checkNotNull(structType, "structType is null");

        Deque<Type> stack = this.stack.get();
        if (stack.contains(structType)) {
            String path = Joiner.on("->").join(transform(concat(stack, ImmutableList.of(structType)), new Function<Type, Object>()
            {
                @Override
                public Object apply(Type input)
                {
                    return TypeToken.of(input).getRawType().getName();
                }
            }));
            throw new IllegalArgumentException("Circular references are not allowed: " + path);
        }

        stack.push(structType);
        try {
            ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(this, structType);
            ThriftStructMetadata structMetadata = builder.build();
            return structMetadata;
        }
        finally {
            Type top = stack.pop();
            checkState(structType.equals(top),
                    "ThriftCatalog circularity detection stack is corrupt: expected %s, but got %s",
                    structType,
                    top);
        }
    }

    private ThriftStructMetadata extractThriftUnionMetadata(Type unionType)
    {
        Preconditions.checkNotNull(unionType, "unionType is null");

        Deque<Type> stack = this.stack.get();
        if (stack.contains(unionType)) {
            String path = Joiner.on("->").join(transform(concat(stack, ImmutableList.of(unionType)), new Function<Type, Object>()
            {
                @Override
                public Object apply(Type input)
                {
                    return TypeToken.of(input).getRawType().getName();
                }
            }));
            throw new IllegalArgumentException("Circular references are not allowed: " + path);
        }

        stack.push(unionType);
        try {
            ThriftUnionMetadataBuilder builder = new ThriftUnionMetadataBuilder(this, unionType);
            ThriftStructMetadata unionMetadata = builder.build();
            return unionMetadata;
        }
        finally {
            Type top = stack.pop();
            checkState(unionType.equals(top),
                    "ThriftCatalog circularity detection stack is corrupt: expected %s, but got %s",
                    unionType,
                    top);
        }
    }

}
