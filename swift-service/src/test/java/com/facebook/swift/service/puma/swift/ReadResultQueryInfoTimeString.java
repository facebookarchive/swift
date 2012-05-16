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
public class ReadResultQueryInfoTimeString {
  private final String startTimeResultWindow;
  private final Map<String, String> columnNameValueMap;

  @ThriftConstructor
  public ReadResultQueryInfoTimeString(
      String startTimeResultWindow,
      Map<String, String> columnNameValueMap
  ) {
    this.startTimeResultWindow = startTimeResultWindow;
    if (columnNameValueMap != null) {
      this.columnNameValueMap = ImmutableMap.copyOf(columnNameValueMap);
    } else {
      this.columnNameValueMap = ImmutableMap.of();
    }
  }

  @ThriftField(1)
  public String getStartTimeResultWindow() {
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

    final ReadResultQueryInfoTimeString that = (ReadResultQueryInfoTimeString) o;

    if (!columnNameValueMap.equals(that.columnNameValueMap)) {
      return false;
    }
    if (startTimeResultWindow != null ? !startTimeResultWindow.equals(that.startTimeResultWindow) : that.startTimeResultWindow != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = startTimeResultWindow != null ? startTimeResultWindow.hashCode() : 0;
    result = 31 * result + columnNameValueMap.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ReadResultQueryInfoTimeString");
    sb.append("{startTimeResultWindow='").append(startTimeResultWindow).append('\'');
    sb.append(", columnNameValueMap=").append(columnNameValueMap);
    sb.append('}');
    return sb.toString();
  }
}
