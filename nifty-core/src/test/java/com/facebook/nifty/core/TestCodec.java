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

import org.apache.thrift.transport.TTransport;
import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestCodec
{
    @Test
    public void testDecoder()
    {
        NettyThriftDecoder decoder = new NettyThriftDecoder();
        ChannelHandlerContext ctx = EasyMock.createMock(ChannelHandlerContext.class);
        Channel channel = EasyMock.createMock(Channel.class);
        EasyMock.expect(ctx.getChannel()).andReturn(channel);
        ctx.sendUpstream(EasyMock.anyObject(ChannelEvent.class));
        EasyMock.replay(ctx, channel);
        try {
            Object rand = new Object();
            Object obj = decoder.decode(ctx, channel, rand);
            Assert.assertEquals(rand, obj);

            Object t = decoder.decode(ctx, channel, ChannelBuffers.EMPTY_BUFFER);
            Assert.assertTrue(t == ChannelBuffers.EMPTY_BUFFER);

            ChannelBuffer channelBuffer = ChannelBuffers.dynamicBuffer();
            channelBuffer.writeBytes(new byte[16], 0, 16);
            Object t1 = decoder.decode(ctx, channel, channelBuffer);
            Assert.assertTrue(t1 instanceof TTransport);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
