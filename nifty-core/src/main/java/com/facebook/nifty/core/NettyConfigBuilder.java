package com.facebook.nifty.core;

import com.google.inject.Inject;
import org.jboss.netty.channel.socket.ServerSocketChannelConfig;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;

import java.lang.reflect.Proxy;

/*
 * Hooks for configuring various parts of Netty.
 */
public class NettyConfigBuilder extends NettyConfigBuilderBase
{

    private final NioSocketChannelConfig socketChannelConfig = (NioSocketChannelConfig) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{NioSocketChannelConfig.class},
            new Magic("child.")
    );
    private final ServerSocketChannelConfig serverSocketChannelConfig = (ServerSocketChannelConfig) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{ServerSocketChannelConfig.class},
            new Magic(""));

    @Inject
    public NettyConfigBuilder()
    {
    }

    public NioSocketChannelConfig getSocketChannelConfig()
    {
        return socketChannelConfig;
    }

    public ServerSocketChannelConfig getServerSocketChannelConfig()
    {
        return serverSocketChannelConfig;
    }
}
