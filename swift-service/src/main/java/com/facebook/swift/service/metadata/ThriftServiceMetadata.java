/*
 * Copyright 2004-present Facebook. All Rights Reserved.
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
import java.util.Set;

import static com.facebook.swift.codec.metadata.ReflectionHelper.findAnnotatedMethods;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getAllClassAnnotations;

@Immutable
public class ThriftServiceMetadata {
  private final String name;
  private final Map<String, ThriftMethodMetadata> methods;

  public ThriftServiceMetadata(Class<?> serviceClass, ThriftCatalog catalog) {
    Preconditions.checkNotNull(serviceClass, "serviceClass is null");
    Set<ThriftService> serviceAnnotations = getAllClassAnnotations(
        serviceClass,
        ThriftService.class
    );
    Preconditions.checkArgument(
        !serviceAnnotations.isEmpty(),
        "Service class %s is not annotated with @ThriftService",
        serviceClass.getName()
    );
    Preconditions.checkArgument(
        serviceAnnotations.size() == 1,
        "Service class %s is has multiple conflicting @ThriftService annotations: %s",
        serviceClass.getName(),
        serviceAnnotations
    );

    ThriftService thriftService = Iterables.getOnlyElement(serviceAnnotations);

    if (thriftService.value().length() == 0) {
      name = serviceClass.getName();
    } else {
      name = thriftService.value();
    }

    ImmutableMap.Builder<String, ThriftMethodMetadata> builder = ImmutableMap.builder();
    for (Method method : findAnnotatedMethods(serviceClass, ThriftMethod.class)) {
      if (method.isAnnotationPresent(ThriftMethod.class)) {
        ThriftMethodMetadata methodMetadata = new ThriftMethodMetadata(
            method,
            catalog
        );
        builder.put(methodMetadata.getName(), methodMetadata);
      }
    }
    methods = builder.build();
  }

  public ThriftServiceMetadata(String name, ThriftMethodMetadata... methods) {
    this.name = name;

    ImmutableMap.Builder<String, ThriftMethodMetadata> builder = ImmutableMap.builder();
    for (ThriftMethodMetadata method : methods) {
      builder.put(method.getName(), method);
    }
    this.methods = builder.build();
  }

  public String getName() {
    return name;
  }

  public ThriftMethodMetadata getMethod(String name) {
    return methods.get(name);
  }

  public Map<String, ThriftMethodMetadata> getMethods() {
    return methods;
  }
}
