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
import org.apache.thrift.TException;

import java.io.Closeable;
import java.nio.ByteBuffer;

@ThriftService(value = "SyncLoadTest")
public interface SyncLoadTest extends Closeable
{
    @ThriftMethod
    public void noop();

    @ThriftMethod(oneway = true)
    public void onewayNoop();

    @ThriftMethod
    public void asyncNoop();

    @ThriftMethod
    public long add(long a, long b);

    @ThriftMethod
    public ByteBuffer echo(ByteBuffer data);

    @ThriftMethod
    public void send(ByteBuffer data);

    @ThriftMethod
    public ByteBuffer recv(long recvBytes);

    @ThriftMethod
    public ByteBuffer sendrecv(ByteBuffer data, long recvBytes);

    @ThriftMethod(oneway = true)
    public void onewaySend(ByteBuffer data);

    @ThriftMethod(oneway = true)
    public void onewayThrow(int code) throws TException;

    @ThriftMethod
    public void throwUnexpected(int code) throws TException;

    @ThriftMethod
    public void throwError(int code) throws LoadError;

    @ThriftMethod
    public void sleep(long microseconds);

    @ThriftMethod(oneway = true)
    public void onewaySleep(long microseconds);

    @ThriftMethod
    public void badBurn(long microseconds);

    @ThriftMethod
    public void badSleep(long microseconds);

    @ThriftMethod(oneway = true)
    public void onewayBurn(long microseconds);

    @ThriftMethod
    public void burn(long microseconds);

    public void close();
}
