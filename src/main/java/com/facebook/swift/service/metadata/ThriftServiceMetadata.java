/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.metadata;

import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

public class ThriftServiceMetadata {
  private final String name;
  private final Map<String, ThriftMethodMetadata> methods;

  public ThriftServiceMetadata(Class<?> serviceClass, ThriftCatalog catalog) {
    Preconditions.checkNotNull(serviceClass, "serviceClass is null");
    ThriftService thriftService = serviceClass.getAnnotation(ThriftService.class);
    Preconditions.checkArgument(thriftService != null,
        "Service class %s is not annotated with @ThriftService",
        serviceClass.getName());

    if (thriftService.value().length() == 0) {
      name = serviceClass.getName();
    } else {
      name = thriftService.value();
    }

    ImmutableMap.Builder<String, ThriftMethodMetadata> builder = ImmutableMap.builder();
    for (Method method : serviceClass.getMethods()) {
      ThriftMethod thriftMethod = method.getAnnotation(ThriftMethod.class);
      if (thriftMethod != null) {
        Preconditions.checkArgument(
            !Modifier.isStatic(method.getModifiers()),
            "Method %s is static", method.toGenericString()
        );

        String methodName;
        if (thriftService.value().length() == 0) {
          methodName = method.getName();
        } else {
          methodName = thriftMethod.value();
        }
        ThriftMethodMetadata methodMetadata = new ThriftMethodMetadata(
            methodName,
            method,
            catalog
        );
        builder.put(methodName, methodMetadata);
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
}
