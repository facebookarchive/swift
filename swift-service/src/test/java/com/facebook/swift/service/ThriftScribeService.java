/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import com.facebook.swift.service.scribe.LogEntry;
import com.facebook.swift.service.scribe.ResultCode;
import com.facebook.swift.service.scribe.scribe;

import java.util.ArrayList;
import java.util.List;

public class ThriftScribeService implements scribe.Iface
{
    private final List<LogEntry> messages = new ArrayList<>();

    public List<LogEntry> getMessages()
    {
        return messages;
    }

    @Override
    public ResultCode Log(List<LogEntry> messages)
    {
        this.messages.addAll(messages);
        return ResultCode.OK;
    }
}
