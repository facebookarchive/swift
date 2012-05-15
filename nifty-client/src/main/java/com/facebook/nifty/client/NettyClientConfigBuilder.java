package com.facebook.nifty.client;

import com.facebook.nifty.core.NettyConfigBuilderBase;
import com.google.inject.Inject;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;

import java.lang.reflect.Proxy;

/*
* Hooks for configuring various parts of Netty.
*/
public class NettyClientConfigBuilder extends NettyConfigBuilderBase {

  private final NioSocketChannelConfig socketChannelConfig = (NioSocketChannelConfig)
    Proxy.newProxyInstance(
      getClass().getClassLoader(),
      new Class<?>[]{NioSocketChannelConfig.class},
      new Magic("")
    );

  @Inject
  public NettyClientConfigBuilder() {
  }

  public NioSocketChannelConfig getSocketChannelConfig() {
    return socketChannelConfig;
  }
}
