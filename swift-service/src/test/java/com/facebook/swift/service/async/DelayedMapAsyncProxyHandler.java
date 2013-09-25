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

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftServer;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DelayedMapAsyncProxyHandler implements DelayedMap.AsyncService
{
    private final DelayedMap.AsyncClient asyncClient;

    public DelayedMapAsyncProxyHandler(ThriftClientManager clientManager, ThriftServer targetServer)
            throws ExecutionException, InterruptedException
    {
        FramedClientConnector connector = new FramedClientConnector(HostAndPort.fromParts("localhost", targetServer.getPort()));
        asyncClient = clientManager.createClient(connector, DelayedMap.AsyncClient.class).get();
    }

    @Override
    public ListenableFuture<Void> putValueSlowly(long timeout,
                               TimeUnit unit,
                               String key,
                               String value)
    {
        return asyncClient.putValueSlowly(timeout, unit, key, value);
    }

    @Override
    public ListenableFuture<String> getValueSlowly(long timeout, TimeUnit unit, String key)
    {
        return asyncClient.getValueSlowly(timeout, unit, key);
    }

    @Override
    public ListenableFuture<List<String>> getMultipleValues(long timeout, TimeUnit unit, List<String> keys)
    {
        return asyncClient.getMultipleValues(timeout, unit, keys);
    }

    @Override
    public ListenableFuture<Void> onewayPutValueSlowly(long timeout, TimeUnit unit, String key, String value)
    {
        return asyncClient.onewayPutValueSlowly(timeout, unit, key, value);
    }
}
