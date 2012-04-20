package com.facebook.nifty.server;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * Converts TNiftyTransport back to ChannelBuffer.
 *
 * @author jaxlaw
 */
public class NettyThriftEncoder extends OneToOneEncoder {
    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (msg instanceof TNiftyTransport) {
            return ((TNiftyTransport) msg).getOutputBuffer();
        }
        return msg;
    }
}
