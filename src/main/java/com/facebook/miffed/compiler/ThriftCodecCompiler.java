/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.compiler;

import com.facebook.miffed.metadata.ThriftCatalog;
import com.facebook.miffed.metadata.ThriftStructMetadata;

import java.lang.reflect.Field;

public class ThriftCodecCompiler
{
    private final ThriftCatalog catalog;

    public ThriftCodecCompiler(ThriftCatalog catalog)
    {
        this.catalog = catalog;
    }

    public <T> ThriftTypeCodec<T> generateThriftTypeCodec(Class<T> type)
    {
        ThriftStructMetadata<?> structMetadata = catalog.getThriftStructMetadata(type);
        Class<ThriftTypeCodec<?>> codecClass = generateClass(structMetadata);
        try {
            Field instanceField = codecClass.getField("INSTANCE");
            Object instance = instanceField.get(null);
            return (ThriftTypeCodec<T>) instance;
        }
        catch (Exception e) {
            throw new IllegalStateException("Generated class is invalid", e);
        }
    }

    private Class<ThriftTypeCodec<?>> generateClass(ThriftStructMetadata<?> metadata)
    {
        throw new UnsupportedOperationException();
    }
}
