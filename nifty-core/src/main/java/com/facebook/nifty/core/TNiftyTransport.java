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

import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

/**
 * Wraps incoming channel buffer into TTransport and provides a output buffer.
 */
public class TNiftyTransport extends TTransport
{
    private final Channel channel;
    private final ChannelBuffer in;
    private final ThriftTransportType thriftTransportType;
    private final ChannelBuffer out;
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = 1024;
    private final int initialReaderIndex;
    private final int initialBufferPosition;
    private int bufferPosition;
    private int bufferEnd;
    private final byte[] buffer;

    public TNiftyTransport(Channel channel,
                           ChannelBuffer in,
                           ThriftTransportType thriftTransportType)
    {
        this.channel = channel;
        this.in = in.duplicate();
        this.thriftTransportType = thriftTransportType;
        this.out = ChannelBuffers.dynamicBuffer(DEFAULT_OUTPUT_BUFFER_SIZE);
        this.initialReaderIndex = in.readerIndex();

        if (!in.hasArray()) {
            buffer = null;
            bufferPosition = 0;
            initialBufferPosition = bufferEnd = -1;
        }
        else {
            buffer = in.array();
            initialBufferPosition = bufferPosition = in.arrayOffset() + in.readerIndex();
            bufferEnd = bufferPosition + in.readableBytes();
        }
    }

    public TNiftyTransport(Channel channel, ThriftMessage message)
    {
        this(channel, message.getBuffer(), message.getTransportType());
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
        if (getBytesRemainingInBuffer() >= 0) {
            int _read = Math.min(getBytesRemainingInBuffer(), length);
            System.arraycopy(getBuffer(), getBufferPosition(), bytes, offset, _read);
            consumeBuffer(_read);
            return _read;
        }
        else {
            int _read = Math.min(in.readableBytes(), length);
            in.readBytes(bytes, offset, _read);
            return _read;
        }
    }

    @Override
    public int readAll(byte[] bytes, int offset, int length) throws TTransportException {
        if (read(bytes, offset, length) < length) {
            throw new TTransportException("Buffer doesn't have enough bytes to read");
        }
        return length;
    }

    @Override
    public void write(byte[] bytes, int offset, int length)
            throws TTransportException
    {
        out.writeBytes(bytes, offset, length);
    }

    public ChannelBuffer getOutputBuffer()
    {
        return out;
    }

    public ThriftTransportType getTransportType() {
        return thriftTransportType;
    }

    @Override
    public void flush()
            throws TTransportException
    {
        // Flush is a no-op: NiftyDispatcher will write the response to the Channel, in order to
        // guarantee ordering of responses when required.
    }

    @Override
    public void consumeBuffer(int len)
    {
        bufferPosition += len;
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP")
    public byte[] getBuffer()
    {
        return buffer;
    }

    @Override
    public int getBufferPosition()
    {
        return bufferPosition;
    }

    @Override
    public int getBytesRemainingInBuffer()
    {
        return bufferEnd - bufferPosition;
    }

    public int getReadByteCount()
    {
        if (getBytesRemainingInBuffer() >= 0) {
            return getBufferPosition() - initialBufferPosition;
        }
        else {
            return in.readerIndex() - initialReaderIndex;
        }
    }

    public int getWrittenByteCount()
    {
        return getOutputBuffer().writerIndex();
    }
}
