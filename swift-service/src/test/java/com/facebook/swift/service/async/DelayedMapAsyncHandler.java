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
package com.facebook.swift.service.async;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DelayedMapAsyncHandler implements DelayedMap.AsyncService
{
    private DelayedMapSyncHandler innerHandler = new DelayedMapSyncHandler();

    @Override
    public ListenableFuture<Void> putValueSlowly(long timeout,
                               TimeUnit unit,
                               String key,
                               String value)
            throws TException
    {
        innerHandler.putValueSlowly(timeout, unit, key, value);
        return Futures.immediateFuture((Void)null);
    }

    @Override
    public ListenableFuture<String> getValueSlowly(long timeout,
                                 TimeUnit unit,
                                 String key)
            throws TException
    {
        return Futures.immediateFuture(innerHandler.getValueSlowly(timeout, unit, key));
    }

    @Override
    public ListenableFuture<List<String>> getMultipleValues(long timeout, TimeUnit unit, List<String> keys)
            throws TException
    {
        return Futures.immediateFuture(innerHandler.getMultipleValues(timeout, unit, keys));
    }
}
