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

import org.apache.thrift.protocol.TProtocolFactory;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;

public class DefaultThriftFrameCodec implements ThriftFrameCodec
{
    private final ThriftFrameDecoder decoder;
    private final ThriftFrameEncoder encoder;

    public DefaultThriftFrameCodec(int maxFrameSize, TProtocolFactory inputProtocolFactory)
    {
        this.decoder = new DefaultThriftFrameDecoder(maxFrameSize, inputProtocolFactory);
        this.encoder = new DefaultThriftFrameEncoder(maxFrameSize);
    }

    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception
    {
        encoder.handleDownstream(ctx, e);
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception
    {
        decoder.handleUpstream(ctx, e);
    }
}
