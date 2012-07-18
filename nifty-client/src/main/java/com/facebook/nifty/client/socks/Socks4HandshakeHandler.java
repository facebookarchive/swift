package com.facebook.nifty.client.socks;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * This handshake handler swap out the channel pipeline with the delegate
 * upon handshake completion.
 */
public class Socks4HandshakeHandler extends SimpleChannelHandler {
  private final SettableChannelFuture channelFuture = new SettableChannelFuture();
  private final ChannelPipelineFactory delegate;

  public Socks4HandshakeHandler(ChannelPipelineFactory delegate) {
    this.delegate = delegate;
  }

  public SettableChannelFuture getChannelFuture() {
    return channelFuture;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    channelFuture.setChannel(ctx.getChannel());
    if (e.getMessage() instanceof ChannelBuffer) {
      ChannelBuffer msg = (ChannelBuffer) e.getMessage();
      if (msg.readableBytes() < 8) {
        channelFuture.setFailure(new IOException("invalid sock server reply length = " + msg.readableBytes()));
      }
      // ignore
      msg.readByte();
      int status = msg.readByte();
      int port = msg.readShort();
      byte[] addr = new byte[4];
      msg.readBytes(addr);

      ctx.getChannel().setAttachment(new InetSocketAddress(InetAddress.getByAddress(addr), port));

      if (status == SocksProtocols.REQUEST_GRANTED) {
        ctx.getPipeline().remove(Socks4ClientBootstrap.FRAME_DECODER);
        ctx.getPipeline().remove(Socks4ClientBootstrap.HANDSHAKE);
        ChannelPipeline delegatePipeline = delegate.getPipeline();
        for (String name : delegatePipeline.getNames()) {
          ctx.getPipeline().addLast(name, delegatePipeline.get(name));
        }
        channelFuture.setSuccess();
      } else {
        channelFuture.setFailure(new IOException("sock server reply failure code :" + Integer.toHexString(status)));
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    channelFuture.setChannel(ctx.getChannel());
    channelFuture.setFailure(e.getCause());
  }
}
