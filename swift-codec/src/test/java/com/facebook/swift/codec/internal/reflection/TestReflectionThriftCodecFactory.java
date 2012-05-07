/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal.reflection;

import com.facebook.swift.codec.AbstractThriftCodecManagerTest;
import com.facebook.swift.codec.ThriftCodecManager;

public class TestReflectionThriftCodecFactory extends AbstractThriftCodecManagerTest {
  @Override
  public ThriftCodecManager createCodecManager() {
    return new ThriftCodecManager(new ReflectionThriftCodecFactory());
  }
}
