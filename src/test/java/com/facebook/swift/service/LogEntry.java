/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

@ThriftStruct
public class LogEntry {
  private final String category;
  private final String message;

  @ThriftConstructor
  public LogEntry(
      @ThriftField(name = "category") String category,
      @ThriftField(name = "message") String message
  ) {
    this.category = category;
    this.message = message;
  }

  @ThriftField(1)
  public String getCategory() {
    return category;
  }

  @ThriftField(2)
  public String getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final LogEntry logEntry = (LogEntry) o;

    if (category != null ? !category.equals(logEntry.category) : logEntry.category != null) {
      return false;
    }
    if (message != null ? !message.equals(logEntry.message) : logEntry.message != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = category != null ? category.hashCode() : 0;
    result = 31 * result + (message != null ? message.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("LogEntryStruct");
    sb.append("{category='").append(category).append('\'');
    sb.append(", message='").append(message).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
