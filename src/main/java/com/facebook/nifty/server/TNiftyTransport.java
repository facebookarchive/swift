package com.facebook.nifty.server;

import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

/**
 * Created with IntelliJ IDEA.
 * User: jaxlaw
 * Date: 4/19/12
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class TNiftyTransport extends TTransport {
    private final Channel channel;
    private final ChannelBuffer buf;
    private final ChannelBuffer out;
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = 1024;

    public TNiftyTransport(Channel channel, ChannelBuffer buf) {
        this.channel = channel;
        this.buf = buf;
        this.out = ChannelBuffers.dynamicBuffer(DEFAULT_OUTPUT_BUFFER_SIZE);
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void open() throws TTransportException {
        // no-op
    }

    @Override
    public void close() {
        // no-op
        channel.close();
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws TTransportException {
        int remaining = buf.readableBytes();
        if (length > remaining) {
            buf.readBytes(bytes, offset, remaining);
            return remaining;
        } else {
            buf.readBytes(bytes, offset, length);
            return length;
        }
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws TTransportException {
        out.writeBytes(bytes, offset, length);
    }

    public ChannelBuffer getOutputBuffer() {
        return out;
    }
}
