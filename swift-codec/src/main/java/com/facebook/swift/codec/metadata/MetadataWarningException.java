/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.metadata;

public class MetadataWarningException extends RuntimeException {
  public MetadataWarningException(String formatString, Object... args) {
    super("Warning: " + String.format(formatString, args));
  }

  public MetadataWarningException(Throwable cause, String formatString, Object... args) {
    super("Warning: " + String.format(formatString, args), cause);
  }
}
