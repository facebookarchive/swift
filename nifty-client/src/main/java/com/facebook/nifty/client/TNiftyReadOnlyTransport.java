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

import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;

/**
 * Wraps incoming channel buffer into TTransport.
 */
public class TNiftyReadOnlyTransport extends TTransport
{
    private final Channel channel;
    private final ChannelBuffer in;

    public TNiftyReadOnlyTransport(Channel channel, ChannelBuffer in)
    {
        this.channel = channel;
        this.in = in;
    }

    @Override
    public boolean isOpen()
    {
        return channel.isOpen();
    }

    @Override
    public void open()
            throws TTransportException
    {
        // no-op
    }

    @Override
    public void close()
    {
        // no-op
        channel.close();
    }

    @Override
    public int read(byte[] bytes, int offset, int length)
            throws TTransportException
    {
        int _read = Math.min(in.readableBytes(), length);
        in.readBytes(bytes, offset, _read);
        return _read;
    }

    @Override
    public void write(byte[] bytes, int offset, int length)
            throws TTransportException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush()
            throws TTransportException
    {
        throw new UnsupportedOperationException();
    }
}
