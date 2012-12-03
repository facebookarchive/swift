package com.facebook.nifty.core;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TType;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class ThriftUnframedDecoder extends FrameDecoder {
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer)
            throws Exception {
        int messageBeginIndex = buffer.readerIndex();
        ChannelBuffer messageBuffer = null;

        try
        {
            TNiftyTransport transport = new TNiftyTransport(channel, buffer);
            TBinaryProtocol protocol = new TBinaryProtocol(transport);

            protocol.readMessageBegin();
            TProtocolUtil.skip(protocol, TType.STRUCT);
            protocol.readMessageEnd();

            messageBuffer = buffer.slice(messageBeginIndex, buffer.readerIndex());
        }
        catch (IndexOutOfBoundsException e)
        {
            buffer.readerIndex(messageBeginIndex);
            return null;
        }
        catch (Throwable th) {
            buffer.readerIndex(messageBeginIndex);
            return null;
        }

        return messageBuffer;
    }
}
