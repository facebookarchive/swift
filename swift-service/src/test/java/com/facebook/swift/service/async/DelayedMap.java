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

import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TException;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DelayedMap
{

    @ThriftService("DelayedMap")
    public static interface Service
    {
        @ThriftMethod
        public String getValueSlowly(long timeout, TimeUnit unit, String key)
                throws TException;

        @ThriftMethod
        public void putValueSlowly(long timeout, TimeUnit unit, String key, String value)
                throws TException;

        @ThriftMethod
        public List<String> getMultipleValues(long timeout, TimeUnit unit, List<String> keys)
                throws TException;

        @ThriftMethod(oneway = true)
        public void onewayPutValueSlowly(long timeout, TimeUnit unit, String key, String value);
    }

    @ThriftService("DelayedMap")
    public static interface AsyncService
    {
        @ThriftMethod
        public ListenableFuture<String> getValueSlowly(long timeout, TimeUnit unit, String key);

        @ThriftMethod
        public ListenableFuture<Void> putValueSlowly(long timeout, TimeUnit unit, String key, String value);

        @ThriftMethod
        public ListenableFuture<List<String>> getMultipleValues(long timeout, TimeUnit unit, List<String> keys);

        @ThriftMethod(oneway = true)
        public ListenableFuture<Void> onewayPutValueSlowly(long timeout, TimeUnit unit, String key, String value);
    }


    @ThriftService("DelayedMap")
    public interface Client extends Closeable
    {
        @ThriftMethod
        public String getValueSlowly(long timeout, TimeUnit unit, String key)
                throws TException;

        @ThriftMethod
        public Void putValueSlowly(long timeout, TimeUnit unit, String key, String value)
                throws TException;

        @ThriftMethod
        public List<String> getMultipleValues(long timeout, TimeUnit unit, List<String> keys)
                throws TException;

        @ThriftMethod(oneway = true)
        public void onewayPutValueSlowly(long timeout, TimeUnit unit, String key, String value);
    }

    @ThriftService("DelayedMap")
    public interface AsyncClient extends Closeable
    {
        @ThriftMethod
        public ListenableFuture<String> getValueSlowly(long timeout, TimeUnit unit, String key);

        @ThriftMethod
        public ListenableFuture<Void> putValueSlowly(long timeout, TimeUnit unit, String key, String value);

        @ThriftMethod
        public ListenableFuture<List<String>> getMultipleValues(long timeout, TimeUnit unit, List<String> keys);

        @ThriftMethod(oneway = true)
        public ListenableFuture<Void> onewayPutValueSlowly(long timeout, TimeUnit unit, String key, String value);
    }
}
