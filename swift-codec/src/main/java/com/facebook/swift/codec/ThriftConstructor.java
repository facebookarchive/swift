/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The constructor Thrift should use when creating the class.  This annotation is also applied to
 * the factory method of a builder class.
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD, CONSTRUCTOR})
public @interface ThriftConstructor
{
}
