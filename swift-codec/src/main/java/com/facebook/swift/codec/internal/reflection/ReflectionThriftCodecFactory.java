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
package com.facebook.swift.codec.internal.reflection;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.internal.ThriftCodecFactory;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;

import javax.annotation.concurrent.Immutable;

import static java.lang.String.format;

/**
 * Creates reflection based thrift codecs.
 */
@Immutable
public class ReflectionThriftCodecFactory implements ThriftCodecFactory
{
    @Override
    public ThriftCodec<?> generateThriftTypeCodec(ThriftCodecManager codecManager, ThriftStructMetadata metadata)
    {
        switch (metadata.getMetadataType()) {
            case STRUCT:
                return new ReflectionThriftStructCodec<>(codecManager, metadata);
            case UNION:
                return new ReflectionThriftUnionCodec<>(codecManager, metadata);
            default:
                throw new IllegalStateException(format("encountered type %s", metadata.getMetadataType()));
        }
    }
}
