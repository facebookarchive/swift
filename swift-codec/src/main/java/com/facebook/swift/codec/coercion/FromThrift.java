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
 * Marks a method as being a conversion from a native Thrift type to a Java Type.  A method with
 * this annotation must be public, static, with a single parameter of a native Thrift type, and can
 * return any Java type other than void.
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD})
public @interface FromThrift {
}
