package com.facebook.nifty.core;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.ServerSocketChannelConfig;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class TestNettyConfigBuilder {

  private int port;

  @BeforeTest(alwaysRun = true)
  public void setup() {
    try {
      ServerSocket s = new ServerSocket();
      s.bind(new InetSocketAddress(0));
      port = s.getLocalPort();
      s.close();
    } catch (IOException e) {
      port = 8080;
    }
  }

  @Test
  public void testNettyConfigBuilder() {
    NettyConfigBuilder configBuilder = new NettyConfigBuilder();

    configBuilder.getServerSocketChannelConfig().setReceiveBufferSize(10000);
    configBuilder.getServerSocketChannelConfig().setBacklog(1000);
    configBuilder.getServerSocketChannelConfig().setReuseAddress(true);

    ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory());
    bootstrap.setOptions(configBuilder.getOptions());
    bootstrap.setPipelineFactory(Channels.pipelineFactory(Channels.pipeline()));
    Channel serverChannel = bootstrap.bind(new InetSocketAddress(port));

    Assert.assertEquals(((ServerSocketChannelConfig)serverChannel.getConfig()).getReceiveBufferSize(), 10000);
    Assert.assertEquals(((ServerSocketChannelConfig)serverChannel.getConfig()).getBacklog(), 1000);
    Assert.assertTrue(((ServerSocketChannelConfig)serverChannel.getConfig()).isReuseAddress());


  }
}
