/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks the method of a Thrift enum that will return the value of the enum constant in Thrift.
 * This must be a public, non-static, no-arg method that returns an int or Integer.  This method
 * must return a constant value.
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface ThriftEnumValue
{
}
