/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal.builtin;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.base.Preconditions;
import org.apache.thrift.protocol.TProtocol;

import javax.annotation.concurrent.Immutable;
import java.util.Set;

@Immutable
public class SetThriftCodec<T> implements ThriftCodec<Set<T>>
{
    private final ThriftCodec<T> elementCodec;
    private final ThriftType type;

    public SetThriftCodec(ThriftType type, ThriftCodec<T> elementCodec)
    {
        Preconditions.checkNotNull(type, "type is null");
        Preconditions.checkNotNull(elementCodec, "elementCodec is null");

        this.type = type;
        this.elementCodec = elementCodec;
    }

    @Override
    public ThriftType getType()
    {
        return type;
    }

    @Override
    public Set<T> read(TProtocol protocol)
            throws Exception
    {
        Preconditions.checkNotNull(protocol, "protocol is null");
        return new TProtocolReader(protocol).readSet(elementCodec);
    }

    @Override
    public void write(Set<T> value, TProtocol protocol)
            throws Exception
    {
        Preconditions.checkNotNull(value, "value is null");
        Preconditions.checkNotNull(protocol, "protocol is null");
        new TProtocolWriter(protocol).writeSet(elementCodec, value);
    }
}
