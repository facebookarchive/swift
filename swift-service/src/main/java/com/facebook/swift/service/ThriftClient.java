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

import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.nifty.client.NiftyClientConnector;
import com.facebook.nifty.client.RequestChannel;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import io.airlift.units.Duration;
import org.weakref.jmx.Managed;

import javax.annotation.Nullable;

import java.util.List;

public class ThriftClient<T>
{
    private final ThriftClientManager clientManager;
    private final Class<T> clientType;
    private final String clientName;
    private final Duration connectTimeout;
    private final Duration receiveTimeout;
    private final Duration readTimeout;
    private final Duration writeTimeout;

    private final HostAndPort socksProxy;
    private final int maxFrameSize;
    private final List<? extends ThriftClientEventHandler> eventHandlers;

    @Inject
    public ThriftClient(ThriftClientManager clientManager, Class<T> clientType)
    {
        this(clientManager, clientType, new ThriftClientConfig(), ThriftClientManager.DEFAULT_NAME, ImmutableList.<ThriftClientEventHandler>of());
    }

    public ThriftClient(
            ThriftClientManager clientManager,
            Class<T> clientType,
            ThriftClientConfig clientConfig,
            String clientName)
    {
        this(clientManager, clientType, clientConfig, clientName, ImmutableList.<ThriftClientEventHandler>of());
    }

    public ThriftClient(
            ThriftClientManager clientManager,
            Class<T> clientType,
            ThriftClientConfig clientConfig,
            String clientName,
            List<? extends ThriftClientEventHandler> eventHandlers)
    {
        this(clientManager,
                clientType,
                clientName,
                clientConfig.getConnectTimeout(),
                clientConfig.getReceiveTimeout(),
                clientConfig.getReadTimeout(),
                clientConfig.getWriteTimeout(),
                clientConfig.getSocksProxy(),
                clientConfig.getMaxFrameSize(),
                eventHandlers);
    }

    public ThriftClient(
            ThriftClientManager clientManager,
            Class<T> clientType,
            String clientName,
            Duration connectTimeout,
            Duration receiveTimeout,
            Duration readTimeout,
            Duration writeTimeout,
            @Nullable HostAndPort socksProxy,
            int maxFrameSize,
            List<? extends ThriftClientEventHandler> eventHandlers)
    {
        Preconditions.checkNotNull(clientManager, "clientManager is null");
        Preconditions.checkNotNull(clientType, "clientInterface is null");
        Preconditions.checkNotNull(clientName, "clientName is null");
        Preconditions.checkNotNull(connectTimeout, "connectTimeout is null");
        Preconditions.checkNotNull(receiveTimeout, "receiveTimeout is null");
        Preconditions.checkNotNull(readTimeout, "readTimeout is null");
        Preconditions.checkNotNull(writeTimeout, "writeTimeout is null");
        Preconditions.checkArgument(maxFrameSize >= 0, "maxFrameSize cannot be negative");
        Preconditions.checkNotNull(eventHandlers, "eventHandlers is null");

        this.clientManager = clientManager;
        this.clientType = clientType;
        this.clientName = clientName;
        this.connectTimeout = connectTimeout;
        this.receiveTimeout = receiveTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.socksProxy = socksProxy;
        this.maxFrameSize = maxFrameSize;
        this.eventHandlers = eventHandlers;
    }

    public ThriftClient<T> withConnectTimeout(Duration connectTimeout)
    {
        return new ThriftClient<>(
                this.clientManager,
                this.clientType,
                this.clientName,
                connectTimeout,
                this.receiveTimeout,
                this.readTimeout,
                this.writeTimeout,
                this.socksProxy,
                this.maxFrameSize,
                this.eventHandlers);
    }

    public ThriftClient<T> withReceiveTimeout(Duration receiveTimeout)
    {
        return new ThriftClient<>(
                this.clientManager,
                this.clientType,
                this.clientName,
                this.connectTimeout,
                receiveTimeout,
                this.readTimeout,
                this.writeTimeout,
                this.socksProxy,
                this.maxFrameSize,
                this.eventHandlers);
    }

    public ThriftClient<T> withReadTimeout(Duration readTimeout)
    {
        return new ThriftClient<>(
                this.clientManager,
                this.clientType,
                this.clientName,
                this.connectTimeout,
                this.receiveTimeout,
                readTimeout,
                this.writeTimeout,
                this.socksProxy,
                this.maxFrameSize,
                this.eventHandlers);
    }

    public ThriftClient<T> withWriteTimeout(Duration writeTimeout)
    {
        return new ThriftClient<>(
                this.clientManager,
                this.clientType,
                this.clientName,
                this.connectTimeout,
                this.receiveTimeout,
                this.readTimeout,
                writeTimeout,
                this.socksProxy,
                this.maxFrameSize,
                this.eventHandlers);
    }

    @Managed
    public String getClientType()
    {
        return clientType.getName();
    }

    @Managed
    public String getClientName()
    {
        return clientName;
    }

    @Managed
    public String getConnectTimeout()
    {
        return connectTimeout.toString();
    }

    @Managed
    public String getReceiveTimeout()
    {
        return receiveTimeout.toString();
    }

    @Managed
    public String getReadTimeout()
    {
        return readTimeout.toString();
    }

    @Managed
    public String getWriteTimeout()
    {
        return writeTimeout.toString();
    }

    @Managed
    public String getSocksProxy()
    {
        if (socksProxy == null) {
            return null;
        }
        return socksProxy.toString();
    }

    @Managed
    public int getMaxFrameSize()
    {
        return maxFrameSize;
    }

    /***
     * Asynchronously connect to a service to create a new client
     * @param connector Connector used to establish the new connection
     * @return Future that will be set to the client once the connection is established
     */
    public ListenableFuture<T> open(NiftyClientConnector<? extends NiftyClientChannel> connector)
    {
        return clientManager.createClient(
                connector,
                clientType,
                connectTimeout,
                receiveTimeout,
                readTimeout,
                writeTimeout,
                maxFrameSize,
                clientName,
                eventHandlers,
                getSocksProxyOrDefault());
    }

    /***
     * Create a new client from an existing connection
     * @param channel Established client connection
     * @return The new client
     */
    public T open(RequestChannel channel)
    {
        return clientManager.createClient(channel, clientType, clientName, eventHandlers);
    }

    private HostAndPort getSocksProxyOrDefault()
    {
        return (socksProxy != null) ? socksProxy : clientManager.getDefaultSocksProxy();
    }
}
