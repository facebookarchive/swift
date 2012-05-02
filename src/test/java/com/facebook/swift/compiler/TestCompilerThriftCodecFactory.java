/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.AbstractThriftCodecManagerTest;
import com.facebook.swift.ThriftCodecManager;

public class TestCompilerThriftCodecFactory extends AbstractThriftCodecManagerTest {
  @Override
  public ThriftCodecManager createCodecManager() {
    return new ThriftCodecManager(new CompilerThriftCodecFactory());
  }
}
