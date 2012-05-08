/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.metadata;

/**
 * ThriftExtraction contains information an extraction point for a single thrift field.
 *
 * Implementations of this interface are expected to be thread safe.
 */
public interface ThriftExtraction {
  short getId();

  String getName();
}
