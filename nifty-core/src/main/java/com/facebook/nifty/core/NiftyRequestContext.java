/*
 * Copyright (C) 2012-2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.nifty.core;

import org.apache.thrift.protocol.TProtocol;

import java.net.SocketAddress;

public class NiftyRequestContext implements RequestContext
{
    private final ConnectionContext connectionContext;
    private final TProtocol inputProtocol;
    private final TProtocol outputProtocol;
    private final TNiftyTransport niftyTransport;

    /**
     * Gets the remote address of the client that made the request
     *
     * @return The client's remote address as a {@link SocketAddress}
     */
    @Override
    public TProtocol getInputProtocol()
    {
        return inputProtocol;
    }

    @Override
    public TProtocol getOutputProtocol()
    {
        return outputProtocol;
    }

    public TNiftyTransport getNiftyTransport()
    {
        return niftyTransport;
    }

    @Override
    public ConnectionContext getConnectionContext()
    {
        return connectionContext;
    }

    NiftyRequestContext(ConnectionContext connectionContext, TProtocol inputProtocol, TProtocol outputProtocol, TNiftyTransport niftyTransport)
    {
        this.connectionContext = connectionContext;
        this.niftyTransport = niftyTransport;
        this.inputProtocol = inputProtocol;
        this.outputProtocol = outputProtocol;
    }
}
