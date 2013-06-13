/*
 * Copyright (C) 2012-2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.nifty.core;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import org.jboss.netty.channel.socket.ServerSocketChannelConfig;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newCachedThreadPool;

/*
 * Hooks for configuring various parts of Netty.
 */
public class NettyServerConfigBuilder extends NettyConfigBuilderBase<NettyServerConfigBuilder>
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
    public NettyServerConfigBuilder()
    {
    }

    /**
     * Returns an implementation of {@link NioSocketChannelConfig} which will be applied to all
     * {@link org.jboss.netty.channel.socket.nio.NioSocketChannel} instances created to manage
     * connections accepted by the server.
     *
     * @return A mutable {@link NioSocketChannelConfig}
     */
    public NioSocketChannelConfig getSocketChannelConfig()
    {
        return socketChannelConfig;
    }

    /**
     * Returns an implementation of {@link ServerSocketChannelConfig}
     * which will be applied to the {@link org.jboss.netty.channel.socket.ServerSocketChannel}
     * the server will use to accept connections.
     *
     * @return A mutable {@link ServerSocketChannelConfig}
     */
    public ServerSocketChannelConfig getServerSocketChannelConfig()
    {
        return serverSocketChannelConfig;
    }

    public NettyServerConfig build()
    {
        Timer timer = getTimer();
        ExecutorService bossExecutor = getBossExecutor();
        int bossThreadCount = getBossThreadCount();
        ExecutorService workerExecutor = getWorkerExecutor();
        int workerThreadCount = getWorkerThreadCount();

        return new NettyServerConfig(
                getBootstrapOptions(),
                timer != null ? timer : buildDefaultTimer(),
                bossExecutor != null ? bossExecutor : buildDefaultBossExecutor(),
                bossThreadCount,
                workerExecutor != null ? workerExecutor : buildDefaultWorkerExecutor(),
                workerThreadCount
        );
    }

    private Timer buildDefaultTimer()
    {
        return new HashedWheelTimer(renamingThreadFactory(threadNamePattern("-timer-%s")));
    }

    private ExecutorService buildDefaultBossExecutor()
    {
        return newCachedThreadPool(renamingThreadFactory(threadNamePattern("-boss-%s")));
    }

    private ExecutorService buildDefaultWorkerExecutor()
    {
        return newCachedThreadPool(renamingThreadFactory(threadNamePattern("-worker-%s")));
    }

    private String threadNamePattern(String suffix)
    {
        String niftyName = getNiftyName();
        return "nifty-server" + (Strings.isNullOrEmpty(niftyName) ? "" : "-" + niftyName) + suffix;
    }

    private ThreadFactory renamingThreadFactory(String nameFormat)
    {
        return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
    }
}
