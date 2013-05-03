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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

public abstract class ThriftFrameEncoder extends OneToOneEncoder
{
    protected abstract ChannelBuffer encode(ChannelHandlerContext ctx, Channel channel, ThriftMessage message)
            throws Exception;

    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object message)
            throws Exception
    {
        if (!(message instanceof ThriftMessage)) {
            return message;
        }

        return encode(ctx, channel, (ThriftMessage) message);
    }
}
