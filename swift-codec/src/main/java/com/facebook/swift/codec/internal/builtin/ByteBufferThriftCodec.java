/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal.builtin;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.base.Preconditions;
import org.apache.thrift.protocol.TProtocol;

import javax.annotation.concurrent.Immutable;
import java.nio.ByteBuffer;

@Immutable
public class ByteBufferThriftCodec implements ThriftCodec<ByteBuffer>
{
    @Override
    public ThriftType getType()
    {
        return ThriftType.STRING;
    }

    @Override
    public ByteBuffer read(TProtocol protocol)
            throws Exception
    {
        Preconditions.checkNotNull(protocol, "protocol is null");
        return protocol.readBinary();
    }

    @Override
    public void write(ByteBuffer value, TProtocol protocol)
            throws Exception
    {
        Preconditions.checkNotNull(value, "value is null");
        Preconditions.checkNotNull(protocol, "protocol is null");
        protocol.writeBinary(value);
    }
}
