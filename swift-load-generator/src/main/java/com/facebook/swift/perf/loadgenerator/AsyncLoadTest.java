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
package com.facebook.swift.perf.loadgenerator;

import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TException;

import java.io.Closeable;
import java.nio.ByteBuffer;

@ThriftService(value = "AsyncLoadTest")
public interface AsyncLoadTest extends Closeable
{
    @ThriftMethod
    public ListenableFuture<Void> noop()
            throws TException;

    @ThriftMethod(oneway = true)
    public ListenableFuture<Void> onewayNoop()
            throws TException;

    @ThriftMethod
    public ListenableFuture<Void> asyncNoop()
            throws TException;

    @ThriftMethod
    public ListenableFuture<Long> add(long a, long b)
            throws TException;

    @ThriftMethod
    public ListenableFuture<ByteBuffer> echo(ByteBuffer data)
            throws TException;

    @ThriftMethod
    public ListenableFuture<Void> send(ByteBuffer data)
            throws TException;

    @ThriftMethod
    public ListenableFuture<ByteBuffer> recv(long recvBytes)
            throws TException;

    @ThriftMethod
    public ListenableFuture<ByteBuffer> sendrecv(ByteBuffer data, long recvBytes)
            throws TException;

    @ThriftMethod(oneway = true)
    public ListenableFuture<Void> onewaySend(ByteBuffer data)
            throws TException;

    @ThriftMethod(oneway = true)
    public ListenableFuture<Void> onewayThrow(int code)
            throws TException;

    @ThriftMethod
    public ListenableFuture<Void> throwUnexpected(int code)
            throws TException;

    @ThriftMethod
    public ListenableFuture<Void> throwError(int code)
            throws TException;

    @ThriftMethod
    public ListenableFuture<Void> sleep(long microseconds)
            throws TException;

    @ThriftMethod(oneway = true)
    public ListenableFuture<Void> onewaySleep(long microseconds)
            throws TException;

    @ThriftMethod
    public ListenableFuture<Void> badBurn(long microseconds)
            throws TException;

    @ThriftMethod
    public ListenableFuture<Void> badSleep(long microseconds)
            throws TException;

    @ThriftMethod(oneway = true)
    public ListenableFuture<Void> onewayBurn(long microseconds)
            throws TException;

    @ThriftMethod
    public ListenableFuture<Void> burn(long microseconds)
            throws TException;

    public void close();
}
