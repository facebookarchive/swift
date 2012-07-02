/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal.compiler;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.internal.ThriftCodecFactory;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;

import javax.annotation.concurrent.Immutable;

/**
 * Creates Thrift codecs directly in byte code.
 */
@Immutable
public class CompilerThriftCodecFactory implements ThriftCodecFactory
{
    private final boolean debug;
    private final DynamicClassLoader classLoader;

    public CompilerThriftCodecFactory()
    {
        this(false);
    }

    public CompilerThriftCodecFactory(boolean debug)
    {
        this(debug, new DynamicClassLoader());
    }

    public CompilerThriftCodecFactory(boolean debug, DynamicClassLoader classLoader)
    {
        this.classLoader = classLoader;
        this.debug = debug;
    }

    @Override
    public <T> ThriftCodec<T> generateThriftTypeCodec(ThriftCodecManager codecManager, ThriftStructMetadata<T> metadata)
    {
        ThriftCodecByteCodeGenerator<T> generator = new ThriftCodecByteCodeGenerator<>(
                codecManager,
                metadata,
                classLoader,
                debug
        );
        return generator.getThriftCodec();
    }
}
