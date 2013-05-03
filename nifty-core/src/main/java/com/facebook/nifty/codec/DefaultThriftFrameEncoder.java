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
package com.facebook.nifty.codec;

import com.facebook.nifty.core.ThriftMessage;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

public class DefaultThriftFrameEncoder extends ThriftFrameEncoder
{
    private final long maxFrameSize;

    public DefaultThriftFrameEncoder(long maxFrameSize)
    {

        this.maxFrameSize = maxFrameSize;
    }

    @Override
    protected ChannelBuffer encode(ChannelHandlerContext ctx,
                                   Channel channel,
                                   ThriftMessage message) throws Exception
    {
        if (message.getBuffer().readableBytes() > maxFrameSize)
        {
            Channels.fireExceptionCaught(ctx, new TooLongFrameException("Frame size exceeded on encode"));
            return ChannelBuffers.EMPTY_BUFFER;
        }

        switch (message.getTransportType()) {
            case UNFRAMED:
                return message.getBuffer();

            case FRAMED:
                ChannelBuffer frameSizeBuffer = ChannelBuffers.buffer(4);
                frameSizeBuffer.writeInt(message.getBuffer().readableBytes());
                return ChannelBuffers.wrappedBuffer(frameSizeBuffer, message.getBuffer());

            case HEADER:
                throw new UnsupportedOperationException("Header transport is not supported");

            default:
                throw new UnsupportedOperationException("Unrecognized transport type");
        }
    }
}
