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

import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.facebook.swift.codec.metadata.ReflectionHelper.findAnnotatedMethods;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getEffectiveClassAnnotations;

@Immutable
public class ThriftServiceMetadata
{
    private final String name;
    private final Map<String, ThriftMethodMetadata> methods;
    private final Map<String, ThriftMethodMetadata> declaredMethods;
    private final ThriftServiceMetadata parentService;

    public ThriftServiceMetadata(Class<?> serviceClass, ThriftCatalog catalog)
    {
        Preconditions.checkNotNull(serviceClass, "serviceClass is null");
        ThriftService thriftService = getThriftServiceAnnotation(serviceClass);

        if (thriftService.value().length() == 0) {
            name = serviceClass.getSimpleName();
        }
        else {
            name = thriftService.value();
        }

        ImmutableMap.Builder<String, ThriftMethodMetadata> builder = ImmutableMap.builder();
        ImmutableMap.Builder<String, ThriftMethodMetadata> declaredBuilder = ImmutableMap.builder();
        for (Method method : findAnnotatedMethods(serviceClass, ThriftMethod.class)) {
            if (method.isAnnotationPresent(ThriftMethod.class)) {
                ThriftMethodMetadata methodMetadata = new ThriftMethodMetadata(method, catalog);
                builder.put(methodMetadata.getName(), methodMetadata);
                if (method.getDeclaringClass().equals(serviceClass)) {
                    declaredBuilder.put(methodMetadata.getName(), methodMetadata);
                }
            }
        }
        methods = builder.build();
        declaredMethods = declaredBuilder.build();

        ThriftServiceMetadata parentService = null;
        for (Class<?> parent : serviceClass.getInterfaces()) {
            if (!getEffectiveClassAnnotations(parent, ThriftService.class).isEmpty()) {
                Preconditions.checkState(parentService == null, "service " + serviceClass.getSimpleName() + " extends multiple services");
                parentService = new ThriftServiceMetadata(parent, catalog);
            }
        }
        this.parentService = parentService;
    }

    public ThriftServiceMetadata(String name, ThriftMethodMetadata... methods)
    {
        this.name = name;

        ImmutableMap.Builder<String, ThriftMethodMetadata> builder = ImmutableMap.builder();
        for (ThriftMethodMetadata method : methods) {
            builder.put(method.getName(), method);
        }
        this.methods = builder.build();
        this.declaredMethods = this.methods;
        this.parentService = null;
    }

    public String getName()
    {
        return name;
    }

    public ThriftMethodMetadata getMethod(String name)
    {
        return methods.get(name);
    }

    public Map<String, ThriftMethodMetadata> getMethods()
    {
        return methods;
    }

    public Map<String, ThriftMethodMetadata> getDeclaredMethods()
    {
        return declaredMethods;
    }

    public static ThriftService getThriftServiceAnnotation(Class<?> serviceClass)
    {
        Set<ThriftService> serviceAnnotations = getEffectiveClassAnnotations(serviceClass, ThriftService.class);
        Preconditions.checkArgument(!serviceAnnotations.isEmpty(), "Service class %s is not annotated with @ThriftService", serviceClass.getName());
        Preconditions.checkArgument(serviceAnnotations.size() == 1,
                "Service class %s has multiple conflicting @ThriftService annotations: %s",
                serviceClass.getName(),
                serviceAnnotations
        );

        return Iterables.getOnlyElement(serviceAnnotations);
    }

    public ThriftServiceMetadata getParentService()
    {
        return parentService;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, methods, parentService);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ThriftServiceMetadata other = (ThriftServiceMetadata) obj;
        return Objects.equals(this.name, other.name) && Objects.equals(this.methods, other.methods) && Objects.equals(this.parentService, other.parentService);
    }
}
