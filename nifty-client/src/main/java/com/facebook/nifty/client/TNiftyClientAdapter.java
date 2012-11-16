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
package com.facebook.nifty.client;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ExceptionEvent;

public abstract class TNiftyClientAdapter implements TNiftyClientListener
{
    @Override
    public void onFrameRead(Channel channel, ChannelBuffer buffer)
    {
        onInput(new TNiftyReadOnlyTransport(channel, buffer));
    }

    @Override
    public void onChannelClosedOrDisconnected(Channel channel)
    {
    }

    @Override
    public void onExceptionEvent(ExceptionEvent e)
    {
    }

    /**
     * called when a frame is ready to be read.
     *
     * @param tNiftyReadOnlyTransport a one-time-use transport for the frame
     */
    public abstract void onInput(TNiftyReadOnlyTransport tNiftyReadOnlyTransport);

}
