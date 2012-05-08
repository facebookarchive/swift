/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.metadata;

/**
 * ThriftInjection contains information an injection point for a single thrift field.
 *
 * Implementation of this interface are expected to be thread safe.
 */
public interface ThriftInjection {
  short getId();

  String getName();
}
