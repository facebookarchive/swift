/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.metadata;

import com.facebook.swift.codec.ThriftField;
import org.testng.annotations.Test;

import static com.facebook.swift.codec.metadata.ReflectionHelper.extractParameterNames;
import static org.testng.Assert.assertEquals;

public class TestReflectionHelper {

  @Test
  public void testExtractParameterNamesNoAnnotations() throws Exception {
    assertEquals(
        extractParameterNames(
            getClass().getDeclaredMethod(
                "noAnnotations",
                String.class,
                String.class,
                String.class
            )
        ),
        new String[]{"a", "b", "c"}
    );
  }
  private static void noAnnotations(String a, String b, String c) {
  }

  @Test
  public void testExtractParameterNamesThriftFieldAnnotation() throws Exception {
    assertEquals(
        extractParameterNames(
            getClass().getDeclaredMethod(
                "thriftFieldAnnotation",
                String.class,
                String.class,
                String.class
            )
        ),
        new String[]{"a", "b", "c"}
    );
  }

  private static void thriftFieldAnnotation(
      @ThriftField(name = "a") String arg0,
      @ThriftField(name = "b") String arg1,
      @ThriftField(name = "c") String arg2
  ) {
  }
}
