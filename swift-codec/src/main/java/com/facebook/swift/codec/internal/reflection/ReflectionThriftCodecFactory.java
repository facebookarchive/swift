/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal.reflection;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.internal.ThriftCodecFactory;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;

/**
 * Creates reflection based thrift codecs.
 */
public class ReflectionThriftCodecFactory implements ThriftCodecFactory {
  @Override
  public <T> ThriftCodec<T> generateThriftTypeCodec(
      ThriftCodecManager codecManager,
      ThriftStructMetadata<T> metadata
  ) {
    return new ReflectionThriftCodec<>(codecManager, metadata);
  }
}
