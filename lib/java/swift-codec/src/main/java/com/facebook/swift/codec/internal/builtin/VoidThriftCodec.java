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
package com.facebook.swift.codec.internal.builtin;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.base.Preconditions;
import org.apache.thrift.protocol.TProtocol;

import javax.annotation.concurrent.Immutable;

/**
 * VoidThriftCodec is a convenience codec used for service invocations that return void type.
 */
@Immutable
public class VoidThriftCodec implements ThriftCodec<Void>
{
    @Override
    public ThriftType getType()
    {
        return ThriftType.VOID;
    }

    /**
     * Always returns null without reading anything from the stream.
     */
    @Override
    public Void read(TProtocol protocol)
            throws Exception
    {
        Preconditions.checkNotNull(protocol, "protocol is null");
        return null;
    }

    /**
     * Always returns without writing to the stream.
     */
    @Override
    public void write(Void value, TProtocol protocol)
            throws Exception
    {
        Preconditions.checkNotNull(protocol, "protocol is null");
    }
}
