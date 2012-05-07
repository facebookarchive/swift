/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.coercion;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method as being a conversion to a native Thrift type from a Java Type.  A method with
 * this annotation must be public, static, with a single parameter of any Java type, and must return
 * a native Thrift type.
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD})
public @interface ToThrift {
}
