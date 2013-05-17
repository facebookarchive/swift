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
package com.facebook.nifty.client;

import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import com.google.common.net.HostAndPort;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public abstract class AbstractClientConnector<T extends NiftyClientChannel>
        implements NiftyClientConnector<T>
{
    private final SocketAddress address;
    private final TDuplexProtocolFactory protocolFactory;

    public AbstractClientConnector(SocketAddress address, TDuplexProtocolFactory protocolFactory)
    {
        this.address = address;
        this.protocolFactory = protocolFactory;
    }

    @Override
    public ChannelFuture connect(ClientBootstrap bootstrap)
    {
        return bootstrap.connect(address);
    }

    @Override
    public String toString()
    {
        return address.toString();
    }

    protected TDuplexProtocolFactory getProtocolFactory()
    {
        return protocolFactory;
    }

    protected static SocketAddress toSocketAddress(HostAndPort address)
    {
        return new InetSocketAddress(address.getHostText(), address.getPort());
    }

    protected static TDuplexProtocolFactory defaultProtocolFactory()
    {
        return TDuplexProtocolFactory.fromSingleFactory(new TBinaryProtocol.Factory());
    }
}
