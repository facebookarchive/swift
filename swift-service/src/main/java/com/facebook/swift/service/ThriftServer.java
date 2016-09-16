/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.service;

import com.facebook.nifty.codec.DefaultThriftFrameCodecFactory;
import com.facebook.nifty.codec.ThriftFrameCodecFactory;
import com.facebook.nifty.core.NettyServerConfig;
import com.facebook.nifty.core.NettyServerConfigBuilder;
import com.facebook.nifty.core.NettyServerTransport;
import com.facebook.nifty.core.NiftyNoOpSecurityFactory;
import com.facebook.nifty.core.NiftySecurityFactory;
import com.facebook.nifty.core.NiftyTimer;
import com.facebook.nifty.core.ThriftServerDef;
import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.nifty.processor.NiftyProcessorFactory;
import com.facebook.nifty.ssl.SslServerConfiguration;
import com.facebook.nifty.ssl.TransportAttachObserver;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TTransport;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerBossPool;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.Timer;
import org.weakref.jmx.Managed;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static com.facebook.nifty.core.ShutdownUtil.shutdownChannelFactory;
import static com.facebook.nifty.core.ShutdownUtil.shutdownExecutor;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import static java.util.concurrent.Executors.newCachedThreadPool;

public class ThriftServer implements Closeable
{

    public static final ImmutableMap<String,TDuplexProtocolFactory> DEFAULT_PROTOCOL_FACTORIES = ImmutableMap.of(
            "binary", TDuplexProtocolFactory.fromSingleFactory(new TBinaryProtocol.Factory()),
            "compact", TDuplexProtocolFactory.fromSingleFactory(new TCompactProtocol.Factory())
    );
    public static final ImmutableMap<String,ThriftFrameCodecFactory> DEFAULT_FRAME_CODEC_FACTORIES = ImmutableMap.of(
            "buffered", (ThriftFrameCodecFactory) new DefaultThriftFrameCodecFactory(),
            "framed", (ThriftFrameCodecFactory) new DefaultThriftFrameCodecFactory()
    );
    public static final ImmutableMap<String, ExecutorService> DEFAULT_WORKER_EXECUTORS = ImmutableMap.of();
    public static final NiftySecurityFactoryHolder DEFAULT_SECURITY_FACTORY = new NiftySecurityFactoryHolder();
    public static final SslServerConfigurationHolder DEFAULT_SSL_SERVER_CONFIGURATION =
            new SslServerConfigurationHolder();
    public static final TransportAttachObserverHolder DEFAULT_TRANSPORT_ATTACH_OBSERVER =
            new TransportAttachObserverHolder();

    private enum State {
        NOT_STARTED,
        RUNNING,
        CLOSED,
    }

    private final NettyServerTransport transport;
    private final int configuredPort;
    private final DefaultChannelGroup allChannels = new DefaultChannelGroup();

    private final Executor workerExecutor;
    private final ExecutorService acceptorExecutor;
    private final ExecutorService ioExecutor;
    private final int acceptorThreads;
    private final int ioThreads;

    private final ServerChannelFactory serverChannelFactory;

    private final SslServerConfiguration sslServerConfiguration;

    private final TransportAttachObserver transportAttachObserver;

    private State state = State.NOT_STARTED;

    public ThriftServer(NiftyProcessor processor)
    {
        this(processor, new ThriftServerConfig());
    }

    public ThriftServer(NiftyProcessor processor, ThriftServerConfig config)
    {
        this(processor, config, new NiftyTimer("thrift"));
    }

    public ThriftServer(NiftyProcessor processor, ThriftServerConfig config, Timer timer)
    {
        this(
                processor,
                config,
                timer,
                DEFAULT_FRAME_CODEC_FACTORIES,
                DEFAULT_PROTOCOL_FACTORIES,
                DEFAULT_WORKER_EXECUTORS,
                DEFAULT_SECURITY_FACTORY,
                DEFAULT_SSL_SERVER_CONFIGURATION,
                DEFAULT_TRANSPORT_ATTACH_OBSERVER);
    }

    public ThriftServer(
            final NiftyProcessor processor,
            ThriftServerConfig config,
            @ThriftServerTimer Timer timer,
            Map<String, ThriftFrameCodecFactory> availableFrameCodecFactories,
            Map<String, TDuplexProtocolFactory> availableProtocolFactories,
            Map<String, ExecutorService> availableWorkerExecutors,
            NiftySecurityFactory securityFactory,
            SslServerConfiguration sslServerConfiguration,
            TransportAttachObserver transportAttachObserver)
    {
        this(
                processor,
                config,
                timer,
                availableFrameCodecFactories,
                availableProtocolFactories,
                availableWorkerExecutors,
                new NiftySecurityFactoryHolder(securityFactory),
                new SslServerConfigurationHolder(sslServerConfiguration),
                new TransportAttachObserverHolder(transportAttachObserver));
    }

    @Inject
    public ThriftServer(
            final NiftyProcessor processor,
            ThriftServerConfig config,
            @ThriftServerTimer Timer timer,
            Map<String, ThriftFrameCodecFactory> availableFrameCodecFactories,
            Map<String, TDuplexProtocolFactory> availableProtocolFactories,
            @ThriftServerWorkerExecutor Map<String, ExecutorService> availableWorkerExecutors,
            NiftySecurityFactoryHolder securityFactoryHolder,
            SslServerConfigurationHolder sslServerConfigurationHolder,
            TransportAttachObserverHolder transportAttachObserverHolder)
    {
        checkNotNull(availableFrameCodecFactories, "availableFrameCodecFactories cannot be null");
        checkNotNull(availableProtocolFactories, "availableProtocolFactories cannot be null");

        NiftyProcessorFactory processorFactory = new NiftyProcessorFactory()
        {
            @Override
            public NiftyProcessor getProcessor(TTransport transport)
            {
                return processor;
            }
        };

        String transportName = config.getTransportName();
        String protocolName = config.getProtocolName();

        checkState(availableFrameCodecFactories.containsKey(transportName), "No available server transport named " + transportName);
        checkState(availableProtocolFactories.containsKey(protocolName), "No available server protocol named " + protocolName);

        configuredPort = config.getPort();

        workerExecutor = config.getOrBuildWorkerExecutor(availableWorkerExecutors);

        acceptorExecutor = newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("thrift-acceptor-%s").build());
        acceptorThreads = config.getAcceptorThreadCount();
        ioExecutor = newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("thrift-io-%s").build());
        ioThreads = config.getIoThreadCount();

        sslServerConfiguration = sslServerConfigurationHolder.sslServerConfiguration;

        transportAttachObserver = transportAttachObserverHolder.transportAttachObserver;

        serverChannelFactory = new NioServerSocketChannelFactory(new NioServerBossPool(acceptorExecutor, acceptorThreads, ThreadNameDeterminer.CURRENT),
                                                                 new NioWorkerPool(ioExecutor, ioThreads, ThreadNameDeterminer.CURRENT));

        ThriftServerDef thriftServerDef = ThriftServerDef.newBuilder()
                                                         .name("thrift")
                                                         .listen(configuredPort)
                                                         .limitFrameSizeTo((int) config.getMaxFrameSize().toBytes())
                                                         .clientIdleTimeout(config.getIdleConnectionTimeout())
                                                         .withProcessorFactory(processorFactory)
                                                         .limitConnectionsTo(config.getConnectionLimit())
                                                         .limitQueuedResponsesPerConnection(config.getMaxQueuedResponsesPerConnection())
                                                         .thriftFrameCodecFactory(availableFrameCodecFactories.get(transportName))
                                                         .protocol(availableProtocolFactories.get(protocolName))
                                                         .withSecurityFactory(securityFactoryHolder.niftySecurityFactory)
                                                         .using(workerExecutor)
                                                         .taskTimeout(config.getTaskExpirationTimeout())
                                                         .queueTimeout(config.getQueueTimeout())
                                                         .withSSLConfiguration(sslServerConfiguration)
                                                         .withTransportAttachObserver(transportAttachObserver)
                                                         .build();

        NettyServerConfigBuilder nettyServerConfigBuilder = NettyServerConfig.newBuilder();

        nettyServerConfigBuilder.getServerSocketChannelConfig().setBacklog(config.getAcceptBacklog());
        nettyServerConfigBuilder.setBossThreadCount(config.getAcceptorThreadCount());
        nettyServerConfigBuilder.setWorkerThreadCount(config.getIoThreadCount());
        nettyServerConfigBuilder.setTimer(timer);
        if (config.getTrafficClass() != 0) {
            nettyServerConfigBuilder.getSocketChannelConfig().setTrafficClass(config.getTrafficClass());
        }

        NettyServerConfig nettyServerConfig = nettyServerConfigBuilder.build();

        transport = new NettyServerTransport(thriftServerDef, nettyServerConfig, allChannels);
    }

    /**
     * A ThriftServer constructor that takes raw Netty configuration parameters
     */
    public ThriftServer(NettyServerConfig nettyServerConfig, ThriftServerDef thriftServerDef)
    {
        configuredPort = thriftServerDef.getServerPort();
        workerExecutor = thriftServerDef.getExecutor();
        acceptorExecutor = nettyServerConfig.getBossExecutor();
        acceptorThreads = nettyServerConfig.getBossThreadCount();
        ioExecutor = nettyServerConfig.getWorkerExecutor();
        ioThreads = nettyServerConfig.getWorkerThreadCount();
        sslServerConfiguration = thriftServerDef.getSslConfiguration();
        transportAttachObserver = thriftServerDef.getTransportAttachObserver();
        serverChannelFactory = new NioServerSocketChannelFactory(new NioServerBossPool(acceptorExecutor, acceptorThreads, ThreadNameDeterminer.CURRENT),
                                                                 new NioWorkerPool(ioExecutor, ioThreads, ThreadNameDeterminer.CURRENT));
        transport = new NettyServerTransport(thriftServerDef, nettyServerConfig, allChannels);
    }

    @Managed
    public Integer getPort()
    {
        if (configuredPort != 0) {
            return configuredPort;
        }
        else {
            return getBoundPort();
        }
    }

    @Managed
    public int getWorkerThreads()
    {
        if (workerExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) workerExecutor).getPoolSize();
        }

        // Not a ThreadPoolExecutor. It may still be an ExecutorService that uses threads to do
        // it's work, but we have no way to ask a generic Executor for the number of threads it is
        // running.
        return 0;
    }

    public Executor getWorkerExecutor()
    {
        return workerExecutor;
    }

    private int getBoundPort()
    {
        // If the server was configured to bind to port 0, a random port will actually be bound instead
        SocketAddress socketAddress = transport.getServerChannel().getLocalAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            return inetSocketAddress.getPort();
        }

        // Cannot determine the randomly assigned port until the server is started
        return 0;
    }

    @Managed
    public int getAcceptorThreads()
    {
        return acceptorThreads;
    }

    @Managed
    public int getIoThreads()
    {
        return ioThreads;
    }

    public synchronized boolean isRunning() {
        return state == State.RUNNING;
    }

    @PostConstruct
    public synchronized ThriftServer start()
    {
        checkState(state != State.CLOSED, "Thrift server is closed");
        if (state == State.NOT_STARTED) {
            transport.start(serverChannelFactory);
            state = State.RUNNING;
        }
        return this;
    }

    @PreDestroy
    @Override
    public synchronized void close()
    {
        if (state == State.CLOSED) {
            return;
        }

        if (state == State.RUNNING) {
            try {
                transport.stop();
            }
            catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }

        // Executors are created in the constructor, so we should shut them down here even if the
        // server was never actually started
        try {
            if (workerExecutor instanceof ExecutorService) {
                shutdownExecutor((ExecutorService) workerExecutor, "workerExecutor");
            }
            shutdownChannelFactory(serverChannelFactory, acceptorExecutor, ioExecutor, allChannels);
        }
        catch (Exception e) {
            Thread.currentThread().interrupt();
        }

        state = State.CLOSED;
    }

    /**
     * Do not use this class. It is only used to workaround Guice not having @Inject(optional=true) for constructor
     * arguments. The class is public because it's used in ThriftServerModule, which is in a different package.
     */
    public static class NiftySecurityFactoryHolder
    {
        @Inject(optional = true) public NiftySecurityFactory niftySecurityFactory = new NiftyNoOpSecurityFactory();

        @Inject
        public NiftySecurityFactoryHolder()
        {
        }

        public NiftySecurityFactoryHolder(NiftySecurityFactory niftySecurityFactory)
        {
            this.niftySecurityFactory = niftySecurityFactory;
        }
    }

    /**
     * Do not use this class. It is only used to workaround Guice not having @Inject(optional=true) for constructor
     * arguments. The class is public because it's used in ThriftServerModule, which is in a different package.
     */
    public static class SslServerConfigurationHolder
    {
        @Inject(optional = true) public SslServerConfiguration sslServerConfiguration = null;

        @Inject
        public SslServerConfigurationHolder()
        {
        }

        public SslServerConfigurationHolder(SslServerConfiguration sslServerConfiguration) {
            this.sslServerConfiguration = sslServerConfiguration;
        }
    }

    /**
     * Do not use this class. It is only used to workaround Guice not having @Inject(optional=true) for constructor
     * arguments. The class is public because it's used in ThriftServerModule, which is in a different package.
     */
    public static class TransportAttachObserverHolder
    {
        @Inject(optional = true) public TransportAttachObserver transportAttachObserver = null;

        @Inject
        public TransportAttachObserverHolder()
        {
        }

        public TransportAttachObserverHolder(TransportAttachObserver transportAttachObserver) {
            this.transportAttachObserver = transportAttachObserver;
        }
    }
}
