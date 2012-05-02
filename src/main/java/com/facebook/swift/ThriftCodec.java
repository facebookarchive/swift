/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift;

import com.facebook.swift.compiler.TProtocolReader;
import com.facebook.swift.compiler.TProtocolWriter;
import com.facebook.swift.metadata.ThriftType;

public interface ThriftCodec<T> {
  public ThriftType getType();

  public T read(TProtocolReader protocol)
    throws Exception;

  public void write(T value, TProtocolWriter protocol)
    throws Exception;
}
