/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
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
}
