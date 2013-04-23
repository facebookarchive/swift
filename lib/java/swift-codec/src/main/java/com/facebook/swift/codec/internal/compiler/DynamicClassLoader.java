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

import com.facebook.swift.codec.ThriftCodec;

/**
 * A ClassLoader that allows for loading of classes from an array of bytes.
 */
public class DynamicClassLoader extends ClassLoader
{
    public DynamicClassLoader()
    {
        this(getDefaultClassLoader());
    }

    public DynamicClassLoader(ClassLoader parent)
    {
        super(parent);
    }

    public Class<?> defineClass(String name, byte[] byteCode)
            throws ClassFormatError
    {
        return defineClass(name, byteCode, 0, byteCode.length);
    }

    private static ClassLoader getDefaultClassLoader()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        classLoader = ThriftCodec.class.getClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return getSystemClassLoader();
    }
}
