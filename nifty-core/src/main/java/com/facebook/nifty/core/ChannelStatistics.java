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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.channel.group.ChannelGroup;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counters for number of channels open, generic traffic stats and maybe cleanup logic here.
 */
public class ChannelStatistics extends SimpleChannelHandler
{
    // TODO : expose these stats somewhere
    private static final AtomicInteger channelCount = new AtomicInteger(0);
    private final AtomicLong bytesRead = new AtomicLong(0);
    private final AtomicLong bytesWritten = new AtomicLong(0);
    private final ChannelGroup allChannels;

    public static final String NAME = ChannelStatistics.class.getSimpleName();

    public ChannelStatistics(ChannelGroup allChannels)
    {
        this.allChannels = allChannels;
    }

    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
            throws Exception
    {
        if (e instanceof ChannelStateEvent) {
            ChannelStateEvent cse = (ChannelStateEvent) e;
            switch (cse.getState()) {
                case OPEN:
                    if (Boolean.TRUE.equals(cse.getValue())) {
                        // connect
                        channelCount.incrementAndGet();
                        allChannels.add(e.getChannel());
                    }
                    else {
                        // disconnect
                        channelCount.decrementAndGet();
                        allChannels.remove(e.getChannel());
                    }
                    break;
                case BOUND:
                    break;
            }
        }

        if (e instanceof UpstreamMessageEvent) {
            UpstreamMessageEvent ume = (UpstreamMessageEvent) e;
            if (ume.getMessage() instanceof ChannelBuffer) {
                ChannelBuffer cb = (ChannelBuffer) ume.getMessage();
                int readableBytes = cb.readableBytes();
                //  compute stats here, bytes read from remote
                bytesRead.getAndAdd(readableBytes);
            }
        }

        ctx.sendUpstream(e);
    }

    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
            throws Exception
    {
        if (e instanceof DownstreamMessageEvent) {
            DownstreamMessageEvent dme = (DownstreamMessageEvent) e;
            if (dme.getMessage() instanceof ChannelBuffer) {
                ChannelBuffer cb = (ChannelBuffer) dme.getMessage();
                int readableBytes = cb.readableBytes();
                // compute stats here, bytes written to remote
                bytesWritten.getAndAdd(readableBytes);
            }
        }
        ctx.sendDownstream(e);
    }

    public static int getChannelCount()
    {
        return channelCount.get();
    }

    public long getBytesRead()
    {
        return bytesRead.get();
    }

    public long getBytesWritten()
    {
        return bytesWritten.get();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception
    {
    }
}



