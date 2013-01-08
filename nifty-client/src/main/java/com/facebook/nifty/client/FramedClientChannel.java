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

import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.util.Timer;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class FramedClientChannel extends AbstractClientChannel {
    public FramedClientChannel(Channel channel, Timer timer) {
        super(channel, timer);
    }

    @Override
    protected ChannelBuffer extractResponse(Object message) {
        if (!(message instanceof ChannelBuffer)) {
            return null;
        }

        ChannelBuffer buffer = (ChannelBuffer) message;
        if (!buffer.readable()) {
            return null;
        }

        return buffer;
    }

    @Override
    protected int extractSequenceId(ChannelBuffer message) throws TTransportException {
        try {
            int sequenceId;
            int stringLength;
            stringLength = message.getInt(4);
            sequenceId = message.getInt(8 + stringLength);
            return sequenceId;
        } catch (Throwable t) {
            throw new TTransportException("Could not find sequenceId in Thrift message");
        }
    }

    @Override
    protected ChannelFuture writeRequest(ChannelBuffer request) {
        return getNettyChannel().write(request);
    }

    public static class Factory implements NiftyClientChannel.Factory<FramedClientChannel> {
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
                    cp.addLast("frameEncoder", new LengthFieldPrepender(4));
                    cp.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(maxFrameSize,
                                                                                0, 4, 0, 4));
                    return cp;
                }
            };
        }
    }
}
