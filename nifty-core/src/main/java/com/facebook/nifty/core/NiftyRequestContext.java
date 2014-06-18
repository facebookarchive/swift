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

import com.facebook.nifty.duplex.TProtocolPair;
import com.facebook.nifty.duplex.TTransportPair;
import org.apache.thrift.protocol.TProtocol;

public class NiftyRequestContext implements RequestContext
{
    private final ConnectionContext connectionContext;
    private final TTransportPair transportPair;
    private final TProtocolPair protocolPair;

    @Override
    public TProtocol getInputProtocol()
    {
        return protocolPair.getInputProtocol();
    }

    @Override
    public TProtocol getOutputProtocol()
    {
        return protocolPair.getOutputProtocol();
    }

    public TChannelBufferInputTransport getInputTransport()
    {
        return (TChannelBufferInputTransport) transportPair.getInputTransport();
    }

    public TChannelBufferOutputTransport getOutputTransport()
    {
        return (TChannelBufferOutputTransport) transportPair.getOutputTransport();
    }

    @Override
    public ConnectionContext getConnectionContext()
    {
        return connectionContext;
    }

    NiftyRequestContext(
            ConnectionContext connectionContext,
            TTransportPair transportPair,
            TProtocolPair protocolPair)
    {
        this.connectionContext = connectionContext;
        this.transportPair = transportPair;
        this.protocolPair = protocolPair;
    }
}
