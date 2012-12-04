/*
 * Copyright (C) 2012 Facebook, Inc.
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

import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.transport.TTransport;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

/**
 * Converts ChannelBuffer into TNiftyTransport.
 */
public class NettyThriftDecoder extends OneToOneDecoder
{
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception
    {
        if (!(msg instanceof ChannelBuffer)) {
            return msg;
        }
        else {
            ChannelBuffer cb = (ChannelBuffer) msg;
            if (cb.readableBytes() > 0) {
                return getTransport(channel, cb);
            }
        }
        return msg;
    }

    protected TTransport getTransport(Channel channel, ChannelBuffer cb)
    {
        ThriftTransportType type = (cb.getUnsignedByte(0) < 0x80) ?
                ThriftTransportType.FRAMED :
                ThriftTransportType.UNFRAMED;
        if (type == ThriftTransportType.FRAMED) {
            cb.skipBytes(4);
        }
        return new TNiftyTransport(channel, cb, type);
    }
}
