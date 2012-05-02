/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.metadata.ThriftType;

public interface ThriftTypeCodec<T> {
  public ThriftType getType();

  public T read(TProtocolReader protocol)
    throws Exception;

  public void write(T value, TProtocolWriter protocol)
    throws Exception;
}
