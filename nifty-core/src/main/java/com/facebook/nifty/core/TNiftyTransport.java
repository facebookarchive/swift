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
    private final ChannelBuffer out;
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = 1024;

    public TNiftyTransport(Channel channel, ChannelBuffer in)
    {
        this.channel = channel;
        this.in = in;
        this.out = ChannelBuffers.dynamicBuffer(DEFAULT_OUTPUT_BUFFER_SIZE);
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
        out.writeBytes(bytes, offset, length);
    }

    public ChannelBuffer getOutputBuffer()
    {
        return out;
    }

    @Override
    public void flush()
            throws TTransportException
    {
        channel.write(out);
    }
}
