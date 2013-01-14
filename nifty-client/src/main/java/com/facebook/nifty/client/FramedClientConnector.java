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

import com.google.common.net.HostAndPort;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.util.Timer;

import java.net.InetSocketAddress;

/***
 * A {@link NiftyClientConnector} specialized for {@link FramedClientConnector}
 */
public class FramedClientConnector extends AbstractClientConnector<FramedClientChannel> {
    // TFramedTransport framing appears at the front of the message
    private static final int LENGTH_FIELD_OFFSET = 0;

    // TFramedTransport framing is four bytes long
    private static final int LENGTH_FIELD_LENGTH = 4;

    // TFramedTransport framing represents message size *not including* framing so no adjustment
    // is necessary
    private static final int LENGTH_ADJUSTMENT = 0;

    // The client expects to see only the message *without* any framing, this strips it off
    private static final int INITIAL_BYTES_TO_STRIP = LENGTH_FIELD_LENGTH;

    public FramedClientConnector(InetSocketAddress address) {
        super(address);
    }

    public FramedClientConnector(HostAndPort address) {
        super(address);
    }

    @Override
    public FramedClientChannel newThriftClientChannel(Channel nettyChannel, Timer timer) {
        FramedClientChannel channel = new FramedClientChannel(nettyChannel, timer);
        channel.getNettyChannel().getPipeline().addLast("thriftHandler", channel);
        return channel;
    }

    @Override
    public ChannelPipelineFactory newChannelPipelineFactory(final int maxFrameSize) {
        return new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline()
                    throws Exception {
                ChannelPipeline cp = Channels.pipeline();
                cp.addLast("frameEncoder", new LengthFieldPrepender(LENGTH_FIELD_LENGTH));
                cp.addLast(
                        "frameDecoder",
                        new LengthFieldBasedFrameDecoder(
                                maxFrameSize,
                                LENGTH_FIELD_OFFSET,
                                LENGTH_FIELD_LENGTH,
                                LENGTH_ADJUSTMENT,
                                INITIAL_BYTES_TO_STRIP));
                return cp;
            }
        };
    }
}
