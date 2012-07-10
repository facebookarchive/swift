/**
 * Copyright 2012 Facebook, Inc.
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

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class ThriftClient<T>
{
    private final ThriftClientManager clientManager;
    private final Class<T> clientInterface;

    @Inject
    public ThriftClient(ThriftClientManager clientManager, Class<T> clientInterface)
    {
        Preconditions.checkNotNull(clientManager, "clientManager is null");
        Preconditions.checkNotNull(clientInterface, "clientInterface is null");

        this.clientManager = clientManager;
        this.clientInterface = clientInterface;
    }

    public T open(HostAndPort address)
            throws TTransportException
    {
        return clientManager.createClient(address, clientInterface);
    }

    public T open(TTransport transport)
            throws TTransportException
    {
        return clientManager.createClient(transport, clientInterface);
    }
}
