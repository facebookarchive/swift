/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.puma.swift;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@ThriftStruct
public class ReadResultQueryInfo {
  private final long startTimeResultWindow;
  private final Map<String, String> columnNameValueMap;

  @ThriftConstructor
  public ReadResultQueryInfo(long startTimeResultWindow, Map<String, String> columnNameValueMap) {
    this.startTimeResultWindow = startTimeResultWindow;
    if (columnNameValueMap != null) {
      this.columnNameValueMap = ImmutableMap.copyOf(columnNameValueMap);
    } else {
      this.columnNameValueMap = null;
    }
  }

  @ThriftField(1)
  public long getStartTimeResultWindow() {
    return startTimeResultWindow;
  }

  @ThriftField(2)
  public Map<String, String> getColumnNameValueMap() {
    return columnNameValueMap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ReadResultQueryInfo that = (ReadResultQueryInfo) o;

    if (startTimeResultWindow != that.startTimeResultWindow) {
      return false;
    }
    if (!columnNameValueMap.equals(that.columnNameValueMap)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = (int) (startTimeResultWindow ^ (startTimeResultWindow >>> 32));
    result = 31 * result + columnNameValueMap.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ReadResultQueryInfo");
    sb.append("{startTimeResultWindow=").append(startTimeResultWindow);
    sb.append(", columnNameValueMap=").append(columnNameValueMap);
    sb.append('}');
    return sb.toString();
  }
}
