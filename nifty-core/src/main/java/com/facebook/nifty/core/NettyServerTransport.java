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

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A core channel the decode framed Thrift message, dispatches to the TProcessor given
 * and then encode message back to Thrift frame.
 */
public class NettyServerTransport implements ExternalResourceReleasable
{
    private static final Logger log = LoggerFactory.getLogger(NettyServerTransport.class);

    private final int port;
    private final ChannelPipelineFactory pipelineFactory;
    private static final int NO_WRITER_IDLE_TIMEOUT = 0;
    private static final int NO_ALL_IDLE_TIMEOUT = 0;
    private ServerBootstrap bootstrap;
    private Channel serverChannel;
    private final ThriftServerDef def;
    private final NettyConfigBuilder configBuilder;

    @Inject
    public NettyServerTransport(
            final ThriftServerDef def,
            NettyConfigBuilder configBuilder,
            final ChannelGroup allChannels,
            final Timer timer)
    {
        this.def = def;
        this.configBuilder = configBuilder;
        this.port = def.getServerPort();
        if (def.isHeaderTransport()) {
            throw new UnsupportedOperationException("ASF version does not support THeaderTransport !");
        }
        else {
            this.pipelineFactory = new ChannelPipelineFactory()
            {
                @Override
                public ChannelPipeline getPipeline()
                        throws Exception
                {
                    ChannelPipeline cp = Channels.pipeline();
                    cp.addLast(ChannelStatistics.NAME, new ChannelStatistics(allChannels));
                    cp.addLast("frameDecoder", new ThriftFrameDecoder(def.getMaxFrameSize(),
                                                                      def.getInProtocolFactory()));
                    if (def.getClientIdleTimeout() != null) {
                        // Add handlers to detect idle client connections and disconnect them
                        cp.addLast("idleTimeoutHandler", new IdleStateHandler(timer,
                                                                              (int)def.getClientIdleTimeout().toMillis(),
                                                                              NO_WRITER_IDLE_TIMEOUT,
                                                                              NO_ALL_IDLE_TIMEOUT,
                                                                              TimeUnit.MILLISECONDS
                                                                              ));
                        cp.addLast("idleDisconnectHandler", new IdleDisconnectHandler());
                    }
                    cp.addLast("dispatcher", new NiftyDispatcher(def));
                    return cp;
                }
            };
        }

    }

    public void start(ServerChannelFactory serverChannelFactory)
    {
        bootstrap = new ServerBootstrap(serverChannelFactory);
        bootstrap.setOptions(configBuilder.getOptions());
        bootstrap.setPipelineFactory(pipelineFactory);
        log.info("starting transport {}:{}", def.getName(), port);
        serverChannel = bootstrap.bind(new InetSocketAddress(port));
    }

    public void stop()
            throws InterruptedException
    {
        if (serverChannel != null) {
            log.info("stopping transport {}:{}", def.getName(), port);
            // first stop accepting
            final CountDownLatch latch = new CountDownLatch(1);
            serverChannel.close().addListener(new ChannelFutureListener()
            {
                @Override
                public void operationComplete(ChannelFuture future)
                        throws Exception
                {
                    // stop and process remaining in-flight invocations
                    if (def.getExecutor() instanceof ExecutorService) {
                        ExecutorService exe = (ExecutorService) def.getExecutor();
                        ShutdownUtil.shutdownExecutor(exe, "dispatcher");
                    }
                    latch.countDown();
                }
            });
            latch.await();
            serverChannel = null;
        }
    }

    public Channel getServerChannel() {
        return serverChannel;
    }

    @Override
    public void releaseExternalResources()
    {
        bootstrap.releaseExternalResources();
    }
}
