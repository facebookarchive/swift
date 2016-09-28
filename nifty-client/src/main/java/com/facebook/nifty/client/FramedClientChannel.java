/*
 * Copyright (C) 2012-2016 Facebook, Inc.
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
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.util.Timer;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class FramedClientChannel extends AbstractClientChannel {
    protected FramedClientChannel(Channel channel, Timer timer, TDuplexProtocolFactory protocolFactory) {
        super(channel, timer, protocolFactory);
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
    protected ChannelFuture writeRequest(ChannelBuffer request) {
        return getNettyChannel().write(request);
    }

}
