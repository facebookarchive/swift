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
package com.facebook.swift.codec;

import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.reflect.TypeToken;
import org.apache.thrift.protocol.TProtocol;

import java.lang.reflect.Type;

/**
 * A placeholder for a{@link ThriftCodec} that defers computation of the real codec
 * until it is actually used, and then just delegates to that codec.
 *
 * This is used to break the cycle when computing the codec for a recursive type
 * tries to compute codecs for all of its fields.
 */
public class DelegateCodec<T> implements ThriftCodec<T>
{
    private final ThriftCodecManager codecManager;
    private final TypeToken<T> typeToken;

    public DelegateCodec(ThriftCodecManager codecManager, Type javaType)
    {
        this.codecManager = codecManager;
        this.typeToken = (TypeToken<T>) TypeToken.of(javaType);
    }

    @Override
    public ThriftType getType()
    {
        return getCodec().getType();
    }

    @Override
    public T read(TProtocol protocol) throws Exception
    {
        return getCodec().read(protocol);
    }

    @Override
    public void write(T value, TProtocol protocol) throws Exception
    {
        getCodec().write(value, protocol);
    }

    private ThriftCodec<T> getCodec()
    {
        ThriftCodec<T> codec = codecManager.getCachedCodecIfPresent(typeToken);
        if (codec == null) {
            throw new IllegalStateException(
                "Tried to encodec/decode using a DelegateCodec before the target codec was " +
                "built (likely a bug in recursive type support)");
        }
        return codec;
    }
}
