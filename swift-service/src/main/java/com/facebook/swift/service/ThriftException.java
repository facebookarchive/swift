/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Provides mapping for Thrift method exceptions
 */
@Documented
@Retention(RUNTIME)
public @interface ThriftException {
  Class<? extends Throwable> type();

  short id();

  String name() default "";
}
