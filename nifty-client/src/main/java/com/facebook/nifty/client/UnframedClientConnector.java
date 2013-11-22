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

import com.facebook.nifty.core.ThriftUnframedDecoder;
import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import com.google.common.net.HostAndPort;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.util.Timer;

import java.net.InetSocketAddress;

public class UnframedClientConnector extends AbstractClientConnector<UnframedClientChannel> {
    public UnframedClientConnector(InetSocketAddress address)
    {
        this(address, defaultProtocolFactory());
    }

    public UnframedClientConnector(HostAndPort address)
    {
        this(address, defaultProtocolFactory());
    }

    public UnframedClientConnector(InetSocketAddress address, TDuplexProtocolFactory protocolFactory)
    {
        super(address, protocolFactory);
    }

    public UnframedClientConnector(HostAndPort address, TDuplexProtocolFactory protocolFactory)
    {
        super(toSocketAddress(address), protocolFactory);
    }

    @Override
    public UnframedClientChannel newThriftClientChannel(Channel nettyChannel, Timer timer)
    {
        UnframedClientChannel channel = new UnframedClientChannel(nettyChannel, timer, getProtocolFactory());
        ChannelPipeline cp = nettyChannel.getPipeline();
        TimeoutHandler.addToPipeline(cp);
        cp.addLast("thriftHandler", channel);
        return channel;
    }

    @Override
    public ChannelPipelineFactory newChannelPipelineFactory(final int maxFrameSize)
    {
        return new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline()
                    throws Exception {
                ChannelPipeline cp = Channels.pipeline();
                TimeoutHandler.addToPipeline(cp);
                cp.addLast("thriftUnframedDecoder", new ThriftUnframedDecoder());
                return cp;
            }
        };
    }
}
