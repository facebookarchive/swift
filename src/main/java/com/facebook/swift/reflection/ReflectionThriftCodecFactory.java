/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.reflection;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.ThriftCodecManager;
import com.facebook.swift.internal.ThriftCodecFactory;
import com.facebook.swift.metadata.ThriftStructMetadata;

public class ReflectionThriftCodecFactory implements ThriftCodecFactory {
  @Override
  public <T> ThriftCodec<T> generateThriftTypeCodec(
      ThriftCodecManager codecManager, ThriftStructMetadata<T> metadata
  ) {
    return new ReflectionThriftCodec<>(codecManager, metadata);
  }
}
