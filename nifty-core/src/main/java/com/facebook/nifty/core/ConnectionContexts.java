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

import org.jboss.netty.channel.Channel;

import com.google.common.base.Preconditions;

public class ConnectionContexts
{
    public static ConnectionContext getContext(Channel channel)
    {
        ConnectionContext context = (ConnectionContext)
                channel.getPipeline()
                .getContext(ConnectionContextHandler.class)
                .getAttachment();
        Preconditions.checkState(context != null, "Context not yet set on channel %s", channel);
        return context;
    }
}
