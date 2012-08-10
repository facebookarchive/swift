/**
 * Copyright 2012 Facebook, Inc.
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
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.thrift.TException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.facebook.swift.codec.metadata.ReflectionHelper.extractParameterNames;

@Immutable
public class ThriftMethodMetadata
{
    private final String name;
    private final ThriftType returnType;
    private final List<ThriftFieldMetadata> parameters;
    private final Method method;
    private final ImmutableMap<Short, ThriftType> exceptions;
    private final boolean oneway;

    public ThriftMethodMetadata(Method method, ThriftCatalog catalog)
    {
        Preconditions.checkNotNull(method, "method is null");
        Preconditions.checkNotNull(catalog, "catalog is null");

        this.method = method;

        ThriftMethod thriftMethod = method.getAnnotation(ThriftMethod.class);
        Preconditions.checkArgument(thriftMethod != null, "Method is not annotated with @ThriftMethod");

        Preconditions.checkArgument(!Modifier.isStatic(method.getModifiers()), "Method %s is static", method.toGenericString());

        Preconditions.checkArgument(throwsTException(method),
                                    "Thrift method %s must declare TException as throwable",
                                    method.toGenericString());

        if (thriftMethod.value().length() == 0) {
            name = method.getName();
        }
        else {
            name = thriftMethod.value();
        }

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

        if (thriftMethod.exception().length > 0) {
            for (ThriftException thriftException : thriftMethod.exception()) {
                exceptions.put(thriftException.id(), catalog.getThriftType(thriftException.type()));
            }
        } else if (method.getExceptionTypes().length == 2) {
            // Catch the case where the method declares exactly TWO thrown types: one must
            // be the generic TException or an ancestor, so if the other is annotated as a
            // @ThriftStruct then we infer a thrift declaration for that remaining exception
            // type
            for (Class<?> exceptionClass : method.getExceptionTypes()) {
                if (exceptionClass.isAnnotationPresent(ThriftStruct.class)) {
                    exceptions.put((short) 1, catalog.getThriftType(exceptionClass));
                }
            }
        }

        return exceptions.build();
    }

    private static boolean throwsTException(Method method) {
        for (Class<?> exceptionClass : method.getExceptionTypes()) {
            if (exceptionClass.isAssignableFrom(TException.class)) {
                return true;
            }
        }
        return false;
    }
}
