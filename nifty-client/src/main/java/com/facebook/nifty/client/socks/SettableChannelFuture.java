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
package com.facebook.nifty.client.socks;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.DefaultChannelFuture;

/**
 * A channel future that allow channel to be set at a later time.
 */
public class SettableChannelFuture extends DefaultChannelFuture
{
    private Channel settableChannel = null;
    private boolean channelIsSet = false;

    public SettableChannelFuture()
    {
        super(null, false);
    }

    public void setChannel(Channel channel)
    {
        if (!channelIsSet) {
            this.settableChannel = channel;
            this.channelIsSet = true;
        }
    }

    @Override
    public Channel getChannel()
    {
        return this.settableChannel;
    }

    @Override
    public boolean setFailure(Throwable cause)
    {
        if (!this.channelIsSet) {
            throw new IllegalStateException("channel not set yet !");
        }
        return super.setFailure(cause);
    }

    @Override
    public boolean setSuccess()
    {
        if (!this.channelIsSet) {
            throw new IllegalStateException("channel not set yet !");
        }
        return super.setSuccess();
    }
}
