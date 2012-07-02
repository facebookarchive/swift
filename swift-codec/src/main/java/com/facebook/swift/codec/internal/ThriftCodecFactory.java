/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;

/**
 * Implementations of this interface are expected to be thread safe.
 */
public interface ThriftCodecFactory
{
    <T> ThriftCodec<T> generateThriftTypeCodec(ThriftCodecManager codecManager, ThriftStructMetadata<T> metadata);
}
