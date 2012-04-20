package com.facebook.nifty.server;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

/**
 * Converts ChannelBuffer into TNiftyTransport.
 *
 * @author jaxlaw
 */
public class NettyThriftDecoder extends OneToOneDecoder {
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (!(msg instanceof ChannelBuffer)) {
            return msg;
        }
        return new TNiftyTransport(channel, (ChannelBuffer) msg);
    }
}
