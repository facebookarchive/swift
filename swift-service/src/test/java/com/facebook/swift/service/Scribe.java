/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import java.util.List;

@ThriftService("scribe")
public interface Scribe extends AutoCloseable {
  @ThriftMethod("Log")
  ResultCode log(List<LogEntry> messages);
}
