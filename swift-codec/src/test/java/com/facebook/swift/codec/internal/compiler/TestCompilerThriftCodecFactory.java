/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.internal.compiler;

import com.facebook.swift.codec.AbstractThriftCodecManagerTest;
import com.facebook.swift.codec.ThriftCodecManager;

public class TestCompilerThriftCodecFactory extends AbstractThriftCodecManagerTest {
  @Override
  public ThriftCodecManager createCodecManager() {
    return new ThriftCodecManager(new CompilerThriftCodecFactory(true));
  }
}
