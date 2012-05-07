/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

public enum Letter {
  A(65), B(66), C(67), D(68);

  private final int asciiValue;

  Letter(int asciiValue) {

    this.asciiValue = asciiValue;
  }

  @ThriftEnumValue
  public int getAsciiValue() {
    return asciiValue;
  }
}
