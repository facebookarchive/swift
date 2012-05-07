/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import java.util.ArrayList;
import java.util.List;

@ThriftService("scribe")
public class SwiftScribe {
  private final List<LogEntry> messages = new ArrayList<>();

  public List<LogEntry> getMessages() {
    return messages;
  }

  @ThriftMethod("Log")
  public ResultCode log(List<LogEntry> messages) {
    this.messages.addAll(messages);
    return ResultCode.OK;
  }
}
