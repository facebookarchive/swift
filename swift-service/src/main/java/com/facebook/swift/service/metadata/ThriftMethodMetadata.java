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
package com.facebook.swift.service.metadata;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;
import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftFieldMetadata;
import com.facebook.swift.codec.metadata.ThriftInjection;
import com.facebook.swift.codec.metadata.ThriftParameterInjection;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TException;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.facebook.swift.codec.metadata.ReflectionHelper.extractParameterNames;
import static com.google.common.base.Preconditions.checkState;

@Immutable
public class ThriftMethodMetadata
{
    private final String name;
    private final ThriftType returnType;
    private final List<ThriftFieldMetadata> parameters;
    private final Method method;
    private final ImmutableMap<Short, ThriftType> exceptions;
    private final ImmutableList<String> documentation;
    private final boolean oneway;

    public ThriftMethodMetadata(Method method, ThriftCatalog catalog)
    {
        Preconditions.checkNotNull(method, "method is null");
        Preconditions.checkNotNull(catalog, "catalog is null");

        this.method = method;

        ThriftMethod thriftMethod = method.getAnnotation(ThriftMethod.class);
        Preconditions.checkArgument(thriftMethod != null, "Method is not annotated with @ThriftMethod");

        Preconditions.checkArgument(!Modifier.isStatic(method.getModifiers()), "Method %s is static", method.toGenericString());

        if (thriftMethod.value().length() == 0) {
            name = method.getName();
        }
        else {
            name = thriftMethod.value();
        }

        documentation = ThriftCatalog.getThriftDocumentation(method);
        returnType = catalog.getThriftType(method.getGenericReturnType());

        ImmutableList.Builder<ThriftFieldMetadata> builder = ImmutableList.builder();
        Type[] parameterTypes = method.getGenericParameterTypes();
        String[] parameterNames = extractParameterNames(method);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int index = 0; index < parameterTypes.length; index++) {
            ThriftField thriftField = null;
            for (Annotation annotation : parameterAnnotations[index]) {
                if (annotation instanceof ThriftField) {
                    thriftField = (ThriftField) annotation;
                    break;
                }
            }

            short parameterId = Short.MIN_VALUE;
            String parameterName = null;
            if (thriftField != null) {
                parameterId = thriftField.value();
                if (!thriftField.name().isEmpty()) {
                    parameterName = thriftField.name();
                }
            }
            if (parameterId == Short.MIN_VALUE) {
                parameterId = (short) (index + 1);
            }
            if (parameterName == null) {
                parameterName = parameterNames[index];
            }

            Type parameterType = parameterTypes[index];

            ThriftType thriftType = catalog.getThriftType(parameterType);

            ThriftFieldMetadata fieldMetadata = new ThriftFieldMetadata(
                    parameterId,
                    thriftType,
                    parameterName,
                    ImmutableList.<ThriftInjection>of(new ThriftParameterInjection(parameterId, parameterName, index, parameterType)),
                    null,
                    null
            );
            builder.add(fieldMetadata);
        }
        parameters = builder.build();

        exceptions = buildExceptionMap(catalog, thriftMethod);

        this.oneway = thriftMethod.oneway();
    }

    public String getName()
    {
        return name;
    }

    public ThriftType getReturnType()
    {
        return returnType;
    }

    public List<ThriftFieldMetadata> getParameters()
    {
        return parameters;
    }

    public Map<Short, ThriftType> getExceptions()
    {
        return exceptions;
    }

    public ThriftType getException(short id)
    {
        return exceptions.get(id);
    }

    public ImmutableList<String> getDocumentation()
    {
        return documentation;
    }

    public Method getMethod()
    {
        return method;
    }

    public boolean getOneway() {
        return oneway;
    }

    private ImmutableMap<Short, ThriftType> buildExceptionMap(ThriftCatalog catalog,
                                                              ThriftMethod thriftMethod) {
        ImmutableMap.Builder<Short, ThriftType> exceptions = ImmutableMap.builder();
        Set<Type> exceptionTypes = new HashSet<>();
        int customExceptionCount = 0;

        if (thriftMethod.exception().length > 0) {
            for (ThriftException thriftException : thriftMethod.exception()) {
                exceptions.put(thriftException.id(), catalog.getThriftType(thriftException.type()));
                checkState(exceptionTypes.add(thriftException.type()), "ThriftMethod.exception contains more than one value for %s", thriftException.type());
            }
        }

        for (Class<?> exceptionClass : method.getExceptionTypes()) {
            if (exceptionClass.isAssignableFrom(TException.class)) {
                // the built-in exception types don't need special treatment
                continue;
            }

            if (exceptionClass.isAnnotationPresent(ThriftStruct.class)) {
                ++customExceptionCount;

                if (!exceptionTypes.contains(exceptionClass)) {
                    // there is no rhyme or reason to the order exception types are given to us,
                    // so we can only infer the id once
                    checkState(customExceptionCount <= 1, "ThriftMethod.exception annotation value must be specified when more than one custom exception is thrown.");
                    exceptions.put((short) 1, catalog.getThriftType(exceptionClass));
                }
            }
        }

        return exceptions.build();
    }

    public boolean isAsync()
    {
        Type returnType = method.getGenericReturnType();
        Class<?> rawType = TypeToken.of(returnType).getRawType();
        return ListenableFuture.class.isAssignableFrom(rawType);
    }
}
