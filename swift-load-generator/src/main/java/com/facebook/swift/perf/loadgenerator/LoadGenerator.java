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

import static com.facebook.swift.service.guice.ThriftClientBinder.thriftClientBinder;

import io.airlift.bootstrap.LifeCycleManager;
import io.airlift.bootstrap.LifeCycleModule;
import io.airlift.configuration.ConfigurationFactory;
import io.airlift.configuration.ConfigurationModule;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.beust.jcommander.JCommander;
import com.facebook.swift.codec.guice.ThriftCodecModule;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.guice.ThriftClientModule;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.Stage;

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
                    new ConfigurationModule(new ConfigurationFactory(ImmutableMap.<String, String>of())),
                    new LifeCycleModule(),
                    new ThriftCodecModule(),
                    new ThriftClientModule(),
                    new Module()
                    {
                        @Override
                        public void configure(Binder binder)
                        {
                            thriftClientBinder(binder).bindThriftClient(AsyncClientWorker.LoadTest.class);
                            thriftClientBinder(binder).bindThriftClient(SyncClientWorker.LoadTest.class);

                            binder.bind(LoadGeneratorCommandLineConfig.class).toInstance(config);
                            binder.bind(LoadGenerator.class).in(Singleton.class);

                            if (!config.asyncMode) {
                                binder.bind(AbstractClientWorker.class).to(SyncClientWorker.class);
                            } else {
                                binder.bind(AbstractClientWorker.class).to(AsyncClientWorker.class);
                            }
                        }
                    }
            );
            injector.getInstance(LifeCycleManager.class).start();
        }
    }

    @Inject
    public LoadGenerator(LoadGeneratorCommandLineConfig config,
                         Provider<AbstractClientWorker> clientWorkerProvider,
                         ThriftClientManager clientManager) {
        this.config = config;
        this.clientWorkerProvider = clientWorkerProvider;
        this.clientManager = clientManager;
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
    public void stop() {
        for (AbstractClientWorker worker : clientWorkers) {
            worker.shutdown();
        }
        loadStatsThread.shutdown();
        clientManager.close();
    }
}
