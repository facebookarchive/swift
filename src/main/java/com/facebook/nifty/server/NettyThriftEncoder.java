package com.facebook.nifty.server;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * Created with IntelliJ IDEA.
 * User: jaxlaw
 * Date: 4/19/12
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
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
