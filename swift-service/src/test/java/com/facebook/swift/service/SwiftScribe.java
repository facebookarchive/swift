/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import java.util.ArrayList;
import java.util.List;

public class SwiftScribe implements Scribe {
  private final List<LogEntry> messages = new ArrayList<>();

  public List<LogEntry> getMessages() {
    return messages;
  }

  @Override
  public ResultCode log(List<LogEntry> messages) {
    this.messages.addAll(messages);
    return ResultCode.OK;
  }

  @Override
  public void close() {
  }
}
