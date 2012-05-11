package com.facebook.nifty.core;

import junit.framework.Assert;
import org.apache.thrift.transport.TTransport;
import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.testng.annotations.Test;

/**
 * Author @jaxlaw
 * Date: 4/20/12
 * Time: 2:38 PM
 */
public class TestCodec {
  @Test(groups = "fast")
  public void testDecoder() {
    NettyThriftDecoder decoder = new NettyThriftDecoder();
    ChannelHandlerContext ctx = EasyMock.createMock(ChannelHandlerContext.class);
    Channel channel = EasyMock.createMock(Channel.class);
    EasyMock.expect(ctx.getChannel()).andReturn(channel);
    ctx.sendUpstream(EasyMock.anyObject(ChannelEvent.class));
    EasyMock.replay(ctx, channel);
    try {
      Object rand = new Object();
      Object obj = decoder.decode(ctx, channel, rand);
      Assert.assertEquals(rand, obj);

      Object t = decoder.decode(ctx, channel, ChannelBuffers.EMPTY_BUFFER);
      Assert.assertTrue(t == ChannelBuffers.EMPTY_BUFFER);

      ChannelBuffer channelBuffer = ChannelBuffers.dynamicBuffer();
      channelBuffer.writeBytes(new byte[16], 0, 16);
      Object t1 = decoder.decode(ctx, channel, channelBuffer);
      Assert.assertTrue(t1 instanceof TTransport);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
