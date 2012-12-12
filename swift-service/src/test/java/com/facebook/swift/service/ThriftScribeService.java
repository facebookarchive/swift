/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
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
