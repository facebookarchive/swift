/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.reflection;

import com.facebook.swift.AbstractThriftCodecManagerTest;
import com.facebook.swift.ThriftCodecManager;

public class TestReflectionThriftCodecFactory extends AbstractThriftCodecManagerTest {
  @Override
  public ThriftCodecManager createCodecManager() {
    return new ThriftCodecManager(new ReflectionThriftCodecFactory());
  }
}
