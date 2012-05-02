/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.internal;

import com.facebook.swift.ThriftCodecManager;
import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftStructMetadata;

public interface ThriftCodecFactory {
  <T> ThriftCodec<T> generateThriftTypeCodec(
      ThriftCodecManager codecManager,
      ThriftStructMetadata<T> metadata
  );
}
