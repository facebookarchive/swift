/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift;

import com.facebook.swift.metadata.ThriftType;
import org.apache.thrift.protocol.TProtocol;

public interface ThriftCodec
{
    <T> T read(Class<T> type, TProtocol protocol)
            throws Exception;

    <T> void write(Class<T> type, T value, TProtocol protocol)
            throws Exception;

    Object read(ThriftType type, TProtocol protocol)
            throws Exception;

    void write(ThriftType type, Object value, TProtocol protocol)
            throws Exception;
}
