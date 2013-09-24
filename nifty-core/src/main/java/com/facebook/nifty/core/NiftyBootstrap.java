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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * A lifecycle object that manages starting up and shutting down multiple core channels.
 */
public class NiftyBootstrap
{
    private final ChannelGroup allChannels;
    private final NettyServerConfig nettyServerConfig;
    private final Map<ThriftServerDef, NettyServerTransport> transports;
    private ExecutorService bossExecutor;
    private ExecutorService workerExecutor;
    private NioServerSocketChannelFactory serverChannelFactory;

    /**
     * This takes a Set of ThriftServerDef. Use Guice Multibinder to inject.
     */
    @Inject
    public NiftyBootstrap(
            Set<ThriftServerDef> thriftServerDefs,
            NettyServerConfig nettyServerConfig,
            ChannelGroup allChannels)
    {
        this.allChannels = allChannels;
        ImmutableMap.Builder<ThriftServerDef, NettyServerTransport> builder = new ImmutableMap.Builder<>();
        this.nettyServerConfig = nettyServerConfig;
        for (ThriftServerDef thriftServerDef : thriftServerDefs) {
            builder.put(thriftServerDef, new NettyServerTransport(thriftServerDef,
                    nettyServerConfig,
                    allChannels));
        }
        transports = builder.build();
    }

    @PostConstruct
    public void start()
    {
        bossExecutor = nettyServerConfig.getBossExecutor();
        workerExecutor = nettyServerConfig.getWorkerExecutor();
        serverChannelFactory = new NioServerSocketChannelFactory(bossExecutor, workerExecutor);
        for (NettyServerTransport transport : transports.values()) {
            transport.start(serverChannelFactory);
        }
    }

    @PreDestroy
    public void stop()
    {
        for (NettyServerTransport transport : transports.values()) {
            try {
                transport.stop();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ShutdownUtil.shutdownChannelFactory(serverChannelFactory, bossExecutor, workerExecutor, allChannels);
    }

    public Map<ThriftServerDef, NiftyMetrics> getNiftyMetrics()
    {
        ImmutableMap.Builder<ThriftServerDef, NiftyMetrics> builder = new ImmutableMap.Builder<>();
        for (Map.Entry<ThriftServerDef, NettyServerTransport> entry : transports.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().getMetrics());
        }
        return builder.build();
    }
}
