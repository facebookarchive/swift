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

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TType;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class ThriftUnframedDecoder extends FrameDecoder {
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer)
            throws Exception {
        int messageBeginIndex = buffer.readerIndex();
        ChannelBuffer messageBuffer = null;

        try
        {
            TNiftyTransport transport = new TNiftyTransport(channel,
                                                            buffer,
                                                            ThriftTransportType.UNFRAMED);
            TBinaryProtocol protocol = new TBinaryProtocol(transport);

            protocol.readMessageBegin();
            TProtocolUtil.skip(protocol, TType.STRUCT);
            protocol.readMessageEnd();

            messageBuffer = buffer.slice(messageBeginIndex, buffer.readerIndex());
        }
        catch (IndexOutOfBoundsException e)
        {
            buffer.readerIndex(messageBeginIndex);
            return null;
        }
        catch (Throwable th) {
            buffer.readerIndex(messageBeginIndex);
            return null;
        }

        return messageBuffer;
    }
}
