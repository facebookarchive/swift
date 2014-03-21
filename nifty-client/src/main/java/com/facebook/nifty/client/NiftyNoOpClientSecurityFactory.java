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

import com.facebook.nifty.core.NiftySecurityHandlers;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class NiftyNoOpClientSecurityFactory implements NiftyClientSecurityFactory
{
    static final ChannelHandler noOpHandler = new SimpleChannelHandler() {
        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
        {
            super.channelOpen(ctx, e);
            ctx.getPipeline().remove(this);
        }
    };

    @Override
    public NiftySecurityHandlers getSecurityHandlers(int maxFrameSize)
    {
        return new NiftySecurityHandlers()
        {
            @Override
            public ChannelHandler getAuthenticationHandler()
            {
                return noOpHandler;
            }

            @Override
            public ChannelHandler getEncryptionHandler()
            {
                return noOpHandler;
            }
        };
    }
}
