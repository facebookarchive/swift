/*
 * Copyright (C) 2014 Facebook, Inc.
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
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftType;
import org.apache.thrift.protocol.TProtocol;

import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class LongArrayThriftCodec
        implements ThriftCodec<long[]>
{
    @Override
    public ThriftType getType()
    {
        return ThriftType.array(ThriftType.I64);
    }

    @Override
    public long[] read(TProtocol protocol)
            throws Exception
    {
        checkNotNull(protocol, "protocol is null");
        return new TProtocolReader(protocol).readI64Array();
    }

    @Override
    public void write(long[] value, TProtocol protocol)
            throws Exception
    {
        checkNotNull(value, "value is null");
        checkNotNull(protocol, "protocol is null");
        new TProtocolWriter(protocol).writeI64Array(value);
    }
}
