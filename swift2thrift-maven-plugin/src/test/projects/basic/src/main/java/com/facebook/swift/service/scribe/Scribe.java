/*
 * Copyright (C) 2015 Facebook, Inc.
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
package com.facebook.swift.service.scribe;

import com.facebook.swift.codec.*;
import com.facebook.swift.codec.ThriftField.Requiredness;
import com.facebook.swift.service.*;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.*;
import java.util.*;

@ThriftService("Scribe")
public interface Scribe
{
    @ThriftService("Scribe")
    public interface Async
    {
        @ThriftMethod(value = "Log")
        ListenableFuture<ResultCode> log(
            @ThriftField(value=1, name="messages", requiredness=Requiredness.NONE) final List<LogEntry> messages
        );
    }
    @ThriftMethod(value = "Log")
    ResultCode log(
        @ThriftField(value=1, name="messages", requiredness=Requiredness.NONE) final List<LogEntry> messages
    ) throws org.apache.thrift.TException;
}
