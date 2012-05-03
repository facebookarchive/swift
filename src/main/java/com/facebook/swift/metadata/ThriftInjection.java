/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

public interface ThriftInjection {
  short getId();

  String getName();

  ThriftToJavaCoercion getCoercion();
}
