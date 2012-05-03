/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.coercion;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({METHOD})
public @interface FromThrift {
}
