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

import com.facebook.nifty.client.FramedClientChannel;
import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.nifty.client.HttpClientConnector;
import com.facebook.nifty.client.NettyClientConfig;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.*;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.channel.Channel;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AsyncTestBase
{
    public static final int MAX_FRAME_SIZE = 0x3fffffff;
    protected ThriftCodecManager codecManager;
    protected ThriftClientManager clientManager;
    public static final Duration NO_CLIENT_CREATION_DELAY = Duration.valueOf("0ms");
    public static final Duration DEFAULT_CLIENT_CREATION_DELAY = Duration.valueOf("10ms");

    protected <T> ListenableFuture<T> createClient(Class<T> clientClass, ThriftServer server)
            throws InterruptedException, ExecutionException, TTransportException
    {
        return createClient(clientClass, server, DEFAULT_CLIENT_CREATION_DELAY);
    }

    protected <T> ListenableFuture<T> createClient(Class<T> clientClass, int port)
            throws InterruptedException, ExecutionException, TTransportException
    {
        return createClient(clientClass, port, DEFAULT_CLIENT_CREATION_DELAY);
    }

    protected <T> ListenableFuture<T> createClient(Class<T> clientClass, ThriftServer server, final Duration connectDelay)
            throws TTransportException, InterruptedException, ExecutionException
    {
        return createClient(clientClass, server.getPort(), connectDelay);
    }

    protected <T> ListenableFuture<T> createClient(Class<T> clientClass, int serverPort, final Duration connectDelay)
            throws TTransportException, InterruptedException, ExecutionException
    {
        HostAndPort address = HostAndPort.fromParts("localhost", serverPort);
        ThriftClientConfig config = new ThriftClientConfig().setConnectTimeout(new Duration(1, TimeUnit.SECONDS))
                                                            .setReceiveTimeout(new Duration(10, TimeUnit.SECONDS))
                                                            .setReadTimeout(new Duration(1, TimeUnit.SECONDS))
                                                            .setWriteTimeout(new Duration(1, TimeUnit.SECONDS));
        FramedClientConnector connector = new FramedClientConnector(address) {
            @Override
            public FramedClientChannel newThriftClientChannel(
                    Channel nettyChannel, NettyClientConfig nettyClientConfig)
            {
                if (connectDelay.toMillis() > 0) {
                    // Introduce an artificial delay to the client creation process, to test
                    // cases where the client future is not set immediately when making async
                    // connections
                    Uninterruptibles.sleepUninterruptibly(connectDelay.toMillis(), TimeUnit.MILLISECONDS);
                }
                return super.newThriftClientChannel(nettyChannel, nettyClientConfig);
            }
        };
        return new ThriftClient<>(clientManager, clientClass, config, "asyncTestClient").open(connector);
    }

    protected <T> ListenableFuture<T> createHttpClient(Class<T> clientClass, int serverPort)
            throws TTransportException, InterruptedException, ExecutionException
    {
        ThriftClientConfig config = new ThriftClientConfig().setConnectTimeout(new Duration(1, TimeUnit.SECONDS))
                                                            .setReceiveTimeout(new Duration(10, TimeUnit.SECONDS))
                                                            .setReadTimeout(new Duration(1, TimeUnit.SECONDS))
                                                            .setWriteTimeout(new Duration(1, TimeUnit.SECONDS));
        HttpClientConnector connector =
                new HttpClientConnector(URI.create("http://localhost:" + serverPort + "/thrift/"));
        return new ThriftClient<>(clientManager, clientClass, config, "asyncTestClient").open(connector);
    }

    protected ThriftServer createTargetServer(int numThreads)
            throws TException, InstantiationException, IllegalAccessException
    {
        DelayedMapSyncHandler handler = new DelayedMapSyncHandler();
        return createServerFromHandler(new ThriftServerConfig().setWorkerThreads(numThreads), handler);
    }

    protected ThriftServer createAsyncServer(int numThreads, ThriftClientManager clientManager, ThriftServer targetServer) throws Exception
    {
        DelayedMapAsyncProxyHandler handler = new DelayedMapAsyncProxyHandler(clientManager, targetServer);
        return createServerFromHandler(new ThriftServerConfig().setWorkerThreads(numThreads), handler);
    }

    protected ThriftServer createServerFromHandler(Object handler)
            throws InstantiationException, IllegalAccessException
    {
        return createServerFromHandler(new ThriftServerConfig(), handler);
    }

    protected ThriftServer createServerFromHandler(ThriftServerConfig config, Object handler)
            throws IllegalAccessException, InstantiationException
    {
        ThriftServiceProcessor processor = new ThriftServiceProcessor(codecManager, ImmutableList.<ThriftEventHandler>of(), handler);
        config.setMaxFrameSize(new DataSize(MAX_FRAME_SIZE, DataSize.Unit.BYTE));

        return new ThriftServer(processor, config).start();
    }
}
