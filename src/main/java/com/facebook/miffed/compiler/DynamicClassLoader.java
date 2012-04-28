/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.compiler;

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
        classLoader = ThriftTypeCodec.class.getClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return getSystemClassLoader();
    }
}
