/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a class as a service that can be exported with Thrift.
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface ThriftService
{
    String value() default "";
}
