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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static com.facebook.swift.codec.metadata.ReflectionHelper.findAnnotatedMethods;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getAllClassAnnotations;

@Immutable
public class ThriftServiceMetadata
{
    private final String name;
    private final Map<String, ThriftMethodMetadata> methods;
    private final ImmutableList<String> documentation;

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

        documentation = ThriftCatalog.getThriftDocumentation(serviceClass);

        ImmutableMap.Builder<String, ThriftMethodMetadata> builder = ImmutableMap.builder();
        for (Method method : findAnnotatedMethods(serviceClass, ThriftMethod.class)) {
            if (method.isAnnotationPresent(ThriftMethod.class)) {
                ThriftMethodMetadata methodMetadata = new ThriftMethodMetadata(method, catalog);
                builder.put(methodMetadata.getName(), methodMetadata);
            }
        }
        methods = builder.build();
    }

    public ThriftServiceMetadata(String name, ThriftMethodMetadata... methods)
    {
        this.name = name;

        ImmutableMap.Builder<String, ThriftMethodMetadata> builder = ImmutableMap.builder();
        for (ThriftMethodMetadata method : methods) {
            builder.put(method.getName(), method);
        }
        this.methods = builder.build();
        this.documentation = ImmutableList.of();
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

    public ImmutableList<String> getDocumentation()
    {
        return documentation;
    }

    public static ThriftService getThriftServiceAnnotation(Class<?> serviceClass)
    {
        Set<ThriftService> serviceAnnotations = getAllClassAnnotations(serviceClass, ThriftService.class);
        Preconditions.checkArgument(!serviceAnnotations.isEmpty(), "Service class %s is not annotated with @ThriftService", serviceClass.getName());
        Preconditions.checkArgument(serviceAnnotations.size() == 1,
                "Service class %s has multiple conflicting @ThriftService annotations: %s",
                serviceClass.getName(),
                serviceAnnotations
        );

        return Iterables.getOnlyElement(serviceAnnotations);
    }
}
