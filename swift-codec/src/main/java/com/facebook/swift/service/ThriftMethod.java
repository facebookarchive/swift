/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method to be exported in a Thrift service.
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface ThriftMethod {
  String value() default "";
}
