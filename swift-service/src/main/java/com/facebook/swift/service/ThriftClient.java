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
import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import io.airlift.units.Duration;
import org.weakref.jmx.Managed;

public class ThriftClient<T>
{
    private final ThriftClientManager clientManager;
    private final Class<T> clientType;
    private final String clientName;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final Duration writeTimeout;

    private final HostAndPort socksProxy;

    @Inject
    public ThriftClient(ThriftClientManager clientManager, Class<T> clientType)
    {
        this(clientManager, clientType, new ThriftClientConfig(), ThriftClientManager.DEFAULT_NAME);
    }

    public ThriftClient(
            ThriftClientManager clientManager,
            Class<T> clientType,
            ThriftClientConfig clientConfig,
            String clientName)
    {
        Preconditions.checkNotNull(clientManager, "clientManager is null");
        Preconditions.checkNotNull(clientType, "clientInterface is null");
        Preconditions.checkNotNull(clientName, "clientName is null");

        this.clientManager = clientManager;
        this.clientType = clientType;
        this.clientName = clientName;
        connectTimeout = clientConfig.getConnectTimeout();
        readTimeout = clientConfig.getReadTimeout();
        writeTimeout = clientConfig.getWriteTimeout();
        socksProxy = clientConfig.getSocksProxy();
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
                readTimeout,
                writeTimeout,
                clientName,
                socksProxy);
    }

    /***
     * Create a new client from an existing connection
     * @param channel Established client connection
     * @return The new client
     */
    public T open(NiftyClientChannel channel)
    {
        return clientManager.createClient(channel, clientType, clientName);
    }
}
