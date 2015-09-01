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
package com.facebook.swift.codec.internal.compiler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Throwables.propagate;

/**
 * Codecs are defined at runtime in the same package as their type parameters. We avoid access
 * issues by defining them in the same classloader.
 */
public class DynamicClassLoader
{
    final ClassLoader delegate;
    private final Method defineClass;

    DynamicClassLoader(ClassLoader delegate)
    {
        this.delegate = delegate;
        try {
            this.defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            this.defineClass.setAccessible(true);
        }
        catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * In normal case, there is a single swift codec generating a given type within the scope of a
     * classloader. This means a codec class would normally be generated only once per type. In
     * case of misconfiguration, this falls back to loading the type instead of propagating an error
     * when the codec already exists.
     */
    Class<?> defineOrLoadClass(String name, byte[] byteCode)
    {
        try {
            return (Class<?>) defineClass.invoke(delegate, name, byteCode, 0, byteCode.length);
        }
        catch (IllegalAccessException e) {
            throw new AssertionError(e); // already set accessible, so this shouldn't happen.
        }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof LinkageError) {
                try {
                    return delegate.loadClass(name);
                }
                catch (ClassNotFoundException cnfe) {
                    // linkage error wasn't due to lost race
                }
            }
            throw propagate(e.getCause());
        }
    }
}
