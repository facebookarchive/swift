/**
 * Copyright 2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.nifty.core;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

public class NiftyExceptionLogger extends LoggingHandler
{
    private static final Logger log = LoggerFactory.getLogger(NiftyExceptionLogger.class);

    @Override
    public void log(ChannelEvent event)
    {
        if (event instanceof ExceptionEvent) {
            ExceptionEvent exceptionEvent = (ExceptionEvent) event;
            SocketAddress remoteAddress = exceptionEvent.getChannel().getRemoteAddress();
            log.error("Exception triggered on channel connected to " + remoteAddress,
                      exceptionEvent.getCause());
        }
    }
}
