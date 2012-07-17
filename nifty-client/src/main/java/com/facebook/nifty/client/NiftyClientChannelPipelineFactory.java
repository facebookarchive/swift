package com.facebook.nifty.client;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;

class NiftyClientChannelPipelineFactory implements ChannelPipelineFactory {
  private final int maxFrameSize;

  NiftyClientChannelPipelineFactory(int maxFrameSize) {
    this.maxFrameSize = maxFrameSize;
  }

  @Override
  public ChannelPipeline getPipeline() throws Exception {
    ChannelPipeline cp = Channels.pipeline();
    cp.addLast("frameEncoder", new LengthFieldPrepender(4));
    cp.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(maxFrameSize, 0, 4, 0, 4));
    return cp;
  }
}
