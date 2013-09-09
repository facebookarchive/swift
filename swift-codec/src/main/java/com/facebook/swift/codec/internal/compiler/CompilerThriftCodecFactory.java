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
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.internal.ThriftCodecFactory;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;

import java.security.AccessController;
import java.security.PrivilegedAction;

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
        this(debug, getPriviledgedClassLoader());
    }

    public CompilerThriftCodecFactory(boolean debug, DynamicClassLoader classLoader)
    {
        this.classLoader = classLoader;
        this.debug = debug;
    }

    @Override
    public ThriftCodec<?> generateThriftTypeCodec(ThriftCodecManager codecManager, ThriftStructMetadata metadata)
    {
        ThriftCodecByteCodeGenerator<?> generator = new ThriftCodecByteCodeGenerator<>(
                codecManager,
                metadata,
                classLoader,
                debug
        );
        return generator.getThriftCodec();
    }

    private static DynamicClassLoader getPriviledgedClassLoader()
    {
        return AccessController.doPrivileged(new PrivilegedAction<DynamicClassLoader>() {
            public DynamicClassLoader run() {
                return new DynamicClassLoader();
            }
        });
    }
}
