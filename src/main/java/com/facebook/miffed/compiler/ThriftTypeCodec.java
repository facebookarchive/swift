/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.compiler;

public interface ThriftTypeCodec<T>
{
    public Class<T> getType();

    public T read(TProtocolReader protocol)
            throws Exception;

    public void write(T value, TProtocolWriter protocol)
            throws Exception;
}
