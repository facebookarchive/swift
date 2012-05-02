/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.internal.compiler.byteCode;

public class CaseStatement {
  public static CaseStatement caseStatement(int key, String label) {
    return new CaseStatement(label, key);
  }

  private final int key;
  private final String label;

  CaseStatement(String label, int key) {
    this.label = label;
    this.key = key;
  }

  public String getLabel() {
    return label;
  }

  public int getKey() {
    return key;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("CaseStatement");
    sb.append("{label='").append(label).append('\'');
    sb.append(", value=").append(key);
    sb.append('}');
    return sb.toString();
  }
}
