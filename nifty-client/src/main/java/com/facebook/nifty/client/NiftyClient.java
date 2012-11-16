/*
 * Copyright (C) 2012 Facebook, Inc.
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
package com.facebook.nifty.client;

import com.facebook.nifty.client.socks.Socks4ClientBootstrap;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.airlift.units.Duration;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NiftyClient implements Closeable
{
    // 1MB default
    private static final int DEFAULT_MAX_FRAME_SIZE = 1048576;
    public static final Duration DEFAULT_CONNECT_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
    public static final Duration DEFAULT_READ_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
    private static final Duration DEFAULT_WRITE_TIMEOUT = new Duration(2, TimeUnit.SECONDS);

    private final NettyClientConfigBuilder configBuilder;
    private final ExecutorService boss;
    private final ExecutorService worker;
    private final int maxFrameSize;
    private final NioClientSocketChannelFactory channelFactory;
    private final InetSocketAddress defaultSocksProxyAddress;
    private final ChannelGroup allChannels = new DefaultChannelGroup();
    private final HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();

    /**
     * Creates a new NiftyClient with defaults : frame size 1MB, 30 secs
     * connect and read timeout and cachedThreadPool for boss and worker.
     */
    public NiftyClient()
    {
        this(DEFAULT_MAX_FRAME_SIZE);
    }

    public NiftyClient(int maxFrameSize)
    {
        this(new NettyClientConfigBuilder(),
                MoreExecutors.getExitingExecutorService(makeThreadPool("netty-boss")),
                MoreExecutors.getExitingExecutorService(makeThreadPool("netty-worker")),
                maxFrameSize,
                null);
    }

    public NiftyClient(NettyClientConfigBuilder configBuilder,
            ExecutorService boss,
            ExecutorService worker,
            int maxFrameSize)
    {
        this(configBuilder, boss, worker, maxFrameSize, null);
    }

    public NiftyClient(
            NettyClientConfigBuilder configBuilder,
            ExecutorService boss,
            ExecutorService worker,
            int maxFrameSize,
            @Nullable InetSocketAddress defaultSocksProxyAddress)
    {
        this.configBuilder = configBuilder;
        this.boss = boss;
        this.worker = worker;
        this.maxFrameSize = maxFrameSize;
        this.defaultSocksProxyAddress = defaultSocksProxyAddress;
        this.channelFactory = new NioClientSocketChannelFactory(boss, worker);
    }

    public <T extends NiftyClientChannel> ListenableFuture<T> connectAsync(
            NiftyClientChannel.Factory<T> clientChannelFactory,
            InetSocketAddress addr)
            throws InterruptedException
    {
        return connectAsync(clientChannelFactory,
                            addr,
                            DEFAULT_CONNECT_TIMEOUT,
                            DEFAULT_READ_TIMEOUT,
                            DEFAULT_WRITE_TIMEOUT,
                            defaultSocksProxyAddress);
    }

    public <T extends NiftyClientChannel> ListenableFuture<T> connectAsync(
            NiftyClientChannel.Factory<T> clientChannelFactory,
            InetSocketAddress addr,
            Duration connectTimeout,
            Duration receiveTimeout,
            Duration sendTimeout)
            throws InterruptedException
    {
        return connectAsync(clientChannelFactory,
                            addr,
                            connectTimeout,
                            receiveTimeout,
                            sendTimeout,
                            defaultSocksProxyAddress);
    }

    public <T extends NiftyClientChannel> ListenableFuture<T> connectAsync(
            NiftyClientChannel.Factory<T> clientChannelFactory,
            InetSocketAddress addr,
            Duration connectTimeout,
            Duration receiveTimeout,
            Duration sendTimeout,
            @Nullable InetSocketAddress socksProxyAddress)
            throws InterruptedException
    {
        ClientBootstrap bootstrap = createClientBootstrap(socksProxyAddress);
        bootstrap.setOptions(configBuilder.getOptions());
        bootstrap.setOption("connectTimeoutMillis", (long)connectTimeout.toMillis());
        bootstrap.setPipelineFactory(clientChannelFactory.newChannelPipelineFactory(maxFrameSize));
        ChannelFuture nettyChannelFuture = bootstrap.connect(addr);
        nettyChannelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Channel channel = future.getChannel();
                if (channel != null && channel.isOpen()) {
                    // Add the channel to allChannels, and set it up to be removed when closed
                    allChannels.add(channel);
                    channel.getCloseFuture().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            Channel channel = future.getChannel();
                            allChannels.remove(channel);
                        }
                    });
                }
            }
        });
        return new TNiftyFuture<T>(clientChannelFactory,
                                   receiveTimeout,
                                   sendTimeout,
                                   nettyChannelFuture);
    }

    // trying to mirror the synchronous nature of TSocket as much as possible here.
    public TNiftyClientTransport connectSync(InetSocketAddress addr)
            throws TTransportException, InterruptedException
    {
        return connectSync(addr, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, DEFAULT_WRITE_TIMEOUT);
    }

    public TNiftyClientTransport connectSync(
            InetSocketAddress addr,
            Duration connectTimeout,
            Duration receiveTimeout,
            Duration sendTimeout)
            throws TTransportException, InterruptedException
    {
        return connectSync(addr, connectTimeout, receiveTimeout, sendTimeout, defaultSocksProxyAddress);
    }

    public TNiftyClientTransport connectSync(
            InetSocketAddress addr,
            Duration connectTimeout,
            Duration receiveTimeout,
            Duration sendTimeout,
            @Nullable InetSocketAddress socksProxyAddress)
            throws TTransportException, InterruptedException
    {
        // TODO: implement send timeout for sync client
        ClientBootstrap bootstrap = createClientBootstrap(socksProxyAddress);
        bootstrap.setOptions(configBuilder.getOptions());
        bootstrap.setOption("connectTimeoutMillis", (long) connectTimeout.toMillis());
        bootstrap.setPipelineFactory(new NiftyClientChannelPipelineFactory(maxFrameSize));
        ChannelFuture f = bootstrap.connect(addr);
        f.await();
        Channel channel = f.getChannel();
        if (f.getCause() != null) {
            String message = String.format("unable to connect to %s:%d %s",
                    addr.getHostName(),
                    addr.getPort(),
                    socksProxyAddress == null ? "" : "via socks proxy at " + socksProxyAddress);
            throw new TTransportException(message, f.getCause());
        }

        if (f.isSuccess() && (channel != null)) {
            if (channel.isOpen()) {
                // Add the channel to allChannels, and set it up to be removed when closed
                allChannels.add(channel);
                channel.getCloseFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        Channel channel = future.getChannel();
                        allChannels.remove(channel);
                    }
                });
            }

            TNiftyClientTransport transport = new TNiftyClientTransport(channel, receiveTimeout);
            channel.getPipeline().addLast("thrift", transport);
            return transport;
        }

        throw new TTransportException(String.format(
                "unknown error connecting to %s:%d %s",
                addr.getHostName(),
                addr.getPort(),
                socksProxyAddress == null ? "" : "via socks proxy at " + socksProxyAddress
        ));
    }

    @Override
    public void close()
    {
        // Stop the timer thread first, so no timeouts can fire during the rest of the
        // shutdown process
        hashedWheelTimer.stop();

        // Close all open channels
        allChannels.close();

        // Shut down client Executors
        boss.shutdownNow();
        worker.shutdownNow();
    }

    private ClientBootstrap createClientBootstrap(InetSocketAddress socksProxyAddress)
    {
        if (socksProxyAddress != null) {
            return new Socks4ClientBootstrap(channelFactory, socksProxyAddress);
        }
        else {
            return new ClientBootstrap(channelFactory);
        }
    }

    private class TNiftyFuture<T extends NiftyClientChannel> extends AbstractFuture<T>
    {
        private TNiftyFuture(final NiftyClientChannel.Factory<T> clientChannelFactory,
                             final Duration receiveTimeout,
                             final Duration sendTimeout,
                             final ChannelFuture channelFuture)
        {
            channelFuture.addListener(new ChannelFutureListener()
            {
                @Override
                public void operationComplete(ChannelFuture future)
                        throws Exception
                {
                    if (future.isSuccess()) {
                        Channel nettyChannel = future.getChannel();
                        T channel = clientChannelFactory.newThriftClientChannel(nettyChannel,
                                                                                hashedWheelTimer);
                        channel.setReceiveTimeout(receiveTimeout);
                        channel.setSendTimeout(sendTimeout);
                        set(channel);
                    }
                    else if (future.isCancelled()) {
                        cancel(true);
                    }
                    else {
                        setException(future.getCause());
                    }
                }
            });
        }
    }

    // just the inline version of Executors.newCachedThreadPool()
    // to make MoreExecutors happy.
    private static ThreadPoolExecutor makeThreadPool(String name)
    {
        return new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
    }
}
