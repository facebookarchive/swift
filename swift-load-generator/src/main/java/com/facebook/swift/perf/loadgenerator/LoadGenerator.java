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
package com.facebook.swift.perf.loadgenerator;

import com.beust.jcommander.JCommander;
import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.nifty.client.NiftyClientConnector;
import com.facebook.nifty.client.UnframedClientConnector;
import com.facebook.swift.codec.guice.ThriftCodecModule;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.guice.ThriftClientModule;
import com.facebook.swift.service.guice.ThriftClientStatsModule;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.inject.*;
import io.airlift.bootstrap.LifeCycleManager;
import io.airlift.bootstrap.LifeCycleModule;
import io.airlift.configuration.ConfigurationFactory;
import io.airlift.configuration.ConfigurationModule;
import io.airlift.jmx.JmxModule;
import io.airlift.node.NodeModule;
import org.weakref.jmx.guice.MBeanModule;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.util.Map;

import static com.facebook.swift.service.guice.ThriftClientBinder.thriftClientBinder;

public class LoadGenerator
{
    private final Provider<AbstractClientWorker> clientWorkerProvider;
    private final LoadGeneratorCommandLineConfig config;
    private static Injector injector;
    private final ThriftClientManager clientManager;
    private LoadStatsThread loadStatsThread;
    private AbstractClientWorker[] clientWorkers;

    public static void main(final String[] args)
            throws Exception
    {
        final LoadGeneratorCommandLineConfig config = new LoadGeneratorCommandLineConfig();
        JCommander jCommander = new JCommander(config, args);

        if (config.displayUsage) {
            jCommander.usage();
        } else {
            injector = Guice.createInjector(
                    Stage.PRODUCTION,
                    new ConfigurationModule(new ConfigurationFactory(buildConfigMap(config))),
                    new LifeCycleModule(),
                    new ThriftCodecModule(),
                    new ThriftClientModule(),
                    new ThriftClientStatsModule(),
                    new NodeModule(),
                    new JmxModule(),
                    new MBeanModule(),
                    new Module()
                    {
                        @Override
                        public void configure(Binder binder)
                        {
                            thriftClientBinder(binder).bindThriftClient(AsyncLoadTest.class);
                            thriftClientBinder(binder).bindThriftClient(SyncLoadTest.class);

                            binder.bind(LoadGeneratorCommandLineConfig.class).toInstance(config);
                            binder.bind(LoadGenerator.class).in(Singleton.class);

                            if (!config.asyncMode) {
                                binder.bind(AbstractClientWorker.class).to(SyncClientWorker.class);
                            } else {
                                binder.bind(AbstractClientWorker.class).to(AsyncClientWorker.class);
                            }

                            TypeLiteral<NiftyClientConnector<? extends NiftyClientChannel>> channelConnectorType =
                                    new TypeLiteral<NiftyClientConnector<? extends NiftyClientChannel>>() {};
                            NiftyClientConnector<? extends NiftyClientChannel> connector;
                            switch (config.transport) {
                                case FRAMED:
                                    connector = new FramedClientConnector(HostAndPort.fromParts(config.serverAddress, config.serverPort));
                                    break;
                                case UNFRAMED:
                                    connector = new UnframedClientConnector(HostAndPort.fromParts(config.serverAddress, config.serverPort));
                                    break;
                                default:
                                    throw new IllegalStateException("Unknown transport");
                            }
                            binder.bind(channelConnectorType).toInstance(connector);
                        }
                    }
            );
            injector.getInstance(LifeCycleManager.class).start();
        }
    }

    @Inject
    public LoadGenerator(
            LoadGeneratorCommandLineConfig config,
            Provider<AbstractClientWorker> clientWorkerProvider,
            ThriftClientManager clientManager)
    {
        this.config = config;
        this.clientWorkerProvider = clientWorkerProvider;
        this.clientManager = clientManager;
    }

    private static Map<String, String> buildConfigMap(LoadGeneratorCommandLineConfig config)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        if (config.connectTimeoutMilliseconds > 0) {
            addParam(builder, "connect-timeout", config.connectTimeoutMilliseconds + "ms");
        }
        if (config.receiveTimeoutMilliseconds > 0) {
            addParam(builder, "read-timeout", config.receiveTimeoutMilliseconds + "ms");
        }
        if (config.sendTimeoutMilliseconds > 0) {
            addParam(builder, "write-timeout", config.sendTimeoutMilliseconds + "ms");
        }
        return builder.build();
    }

    private static void addParam(ImmutableMap.Builder<String, String> builder, String param, String value)
    {
        builder.put("SyncLoadTest.thrift.client." + param, value);
        builder.put("AsyncLoadTest.thrift.client." + param, value);
    }

    @PostConstruct
    public void start()
            throws Exception
    {
        clientWorkers = new AbstractClientWorker[config.numThreads];

        for (int i = 0; i < config.numThreads; i++) {
            clientWorkers[i] = clientWorkerProvider.get();
        }

        loadStatsThread = new LoadStatsThread(clientWorkers);
        loadStatsThread.start();

        // For fair measurement, start the client workers *after* the monitor has already started
        for (AbstractClientWorker worker : clientWorkers) {
            worker.run();
        }
    }

    @PreDestroy
    public void stop()
    {
        for (AbstractClientWorker worker : clientWorkers) {
            worker.shutdown();
        }
        loadStatsThread.shutdown();
        clientManager.close();
    }
}
