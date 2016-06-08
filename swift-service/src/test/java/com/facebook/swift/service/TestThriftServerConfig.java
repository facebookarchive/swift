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

import com.facebook.swift.codec.guice.ThriftCodecModule;
import com.facebook.swift.service.guice.ThriftServerModule;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.configuration.ConfigBinder;
import io.airlift.configuration.testing.ConfigAssertions;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.facebook.swift.service.guice.ThriftServerModule.bindWorkerExecutor;
import static com.facebook.swift.service.guice.ThriftServiceExporter.thriftServerBinder;
import static io.airlift.configuration.ConfigBinder.configBinder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TestThriftServerConfig
{
    @Test
    public void testDefaults()
    {
        ConfigAssertions.assertRecordedDefaults(
                ConfigAssertions.recordDefaults(ThriftServerConfig.class)
                        .setBindAddress("localhost")
                        .setAcceptBacklog(1024)
                        .setMaxFrameSize(DataSize.valueOf("64MB"))
                        .setPort(0)
                        .setConnectionLimit(0)
                        .setWorkerThreads(200)
                        .setAcceptorThreadCount(1)
                        .setIoThreadCount(2 * Runtime.getRuntime().availableProcessors())
                        .setIdleConnectionTimeout(Duration.valueOf("60s"))
                        .setTransportName("framed")
                        .setProtocolName("binary")
                        .setWorkerExecutorKey(null)
                        .setTaskExpirationTimeout(Duration.valueOf("5s"))
                        .setQueueTimeout(null)
                        .setMaxQueuedRequests(null)
                        .setMaxQueuedResponsesPerConnection(16)
                        .setTrafficClass(0)
        );
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("thrift.port", "12345")
                .put("thrift.max-frame-size", "333kB")
                .put("thrift.bind-address", "127.0.0.1")
                .put("thrift.accept-backlog", "7777")
                .put("thrift.threads.max", "111")
                .put("thrift.acceptor-threads.count", "3")
                .put("thrift.io-threads.count", "27")
                .put("thrift.idle-connection-timeout", "157ms")
                .put("thrift.connection-limit", "1111")
                .put("thrift.worker-executor-key", "my-executor")
                .put("thrift.transport", "buffered")
                .put("thrift.protocol", "compact")
                .put("thrift.task-expiration-timeout", "10s")
                .put("thrift.max-queued-requests", "1000")
                .put("thrift.max-queued-responses-per-connection", "32")
                .put("thrift.queue-timeout", "167ms")
                .put("thrift.traffic-class", "35")
                .build();

        ThriftServerConfig expected = new ThriftServerConfig()
                .setPort(12345)
                .setMaxFrameSize(DataSize.valueOf("333kB"))
                .setBindAddress("127.0.0.1")
                .setAcceptBacklog(7777)
                .setWorkerThreads(111)
                .setAcceptorThreadCount(3)
                .setIoThreadCount(27)
                .setIdleConnectionTimeout(Duration.valueOf("157ms"))
                .setConnectionLimit(1111)
                .setWorkerExecutorKey("my-executor")
                .setTransportName("buffered")
                .setProtocolName("compact")
                .setTaskExpirationTimeout(Duration.valueOf("10s"))
                .setMaxQueuedRequests(1000)
                .setMaxQueuedResponsesPerConnection(32)
                .setQueueTimeout(Duration.valueOf("167ms"))
                .setTrafficClass(35);

        ConfigAssertions.assertFullMapping(properties, expected);
    }

    @Test
    public void testGuiceInjection()
            throws Exception
    {
        Bootstrap bootstrap = new Bootstrap(new AbstractModule() {
            @Override
            protected void configure()
            {
                configBinder(binder()).bindConfig(ThriftServerConfig.class);
            }
        });

        Map<String, String> properties = ImmutableMap.of();

        Injector injector =
                bootstrap.doNotInitializeLogging()
                .strictConfig()
                .setRequiredConfigurationProperties(properties)
                .initialize();

        ThriftServerConfig config = injector.getInstance(ThriftServerConfig.class);
        assertNotNull(config);
    }

    @Test
    public void testWorkerThreadsConfiguration()
            throws Exception
    {
        final int WORKER_THREAD_COUNT = 43;

        Bootstrap bootstrap = new Bootstrap(
                new ThriftCodecModule(),
                new ThriftServerModule(),
                new AbstractModule()
                {
                    @Override
                    protected void configure()
                    {
                        bind(ExampleService.class);
                        thriftServerBinder(binder()).exportThriftService(ExampleService.class);
                    }
                }
        );

        Map<String, String> properties = ImmutableMap.of(
                "thrift.threads.max", Integer.toString(WORKER_THREAD_COUNT)
        );

        Injector injector =
                bootstrap.doNotInitializeLogging()
                         .strictConfig()
                         .setRequiredConfigurationProperties(properties)
                         .initialize();

        ThriftServer server = injector.getInstance(ThriftServer.class);
        Executor executor = server.getWorkerExecutor();
        assertTrue(executor instanceof ThreadPoolExecutor);
        assertEquals(((ThreadPoolExecutor)executor).getMaximumPoolSize(), WORKER_THREAD_COUNT);
    }

    @Test
    public void testWorkerExecutorConfiguration()
            throws Exception
    {
        final int WORKER_THREAD_COUNT = 43;
        final ExecutorService myExecutor = Executors.newFixedThreadPool(WORKER_THREAD_COUNT);

        Bootstrap bootstrap = new Bootstrap(
                new ThriftCodecModule(),
                overrideThriftServerModuleWithWorkerExecutorInstance(myExecutor),
                new AbstractModule()
                {
                    @Override
                    protected void configure()
                    {
                        bind(ExampleService.class);
                        thriftServerBinder(binder()).exportThriftService(ExampleService.class);
                    }
                }
        );

        Map<String, String> properties = ImmutableMap.of();

        Injector injector =
                bootstrap.doNotInitializeLogging()
                         .strictConfig()
                         .setRequiredConfigurationProperties(properties)
                         .initialize();

        ThriftServer server = injector.getInstance(ThriftServer.class);
        assertEquals(server.getWorkerExecutor(), myExecutor);
    }

    @Test
    public void testWorkerExecutorKeyConfiguration()
            throws Exception
    {
        final int WORKER_THREAD_COUNT = 44;
        final ExecutorService myExecutor = Executors.newFixedThreadPool(WORKER_THREAD_COUNT);

        Bootstrap bootstrap = new Bootstrap(
                new ThriftCodecModule(),
                new ThriftServerModule(),
                new AbstractModule()
                {
                    @Override
                    protected void configure()
                    {
                        bind(ExampleService.class);

                        bindWorkerExecutor(binder(), "my-executor", myExecutor);
                        thriftServerBinder(binder()).exportThriftService(ExampleService.class);
                    }
                }
        );

        Map<String, String> properties = ImmutableMap.of(
                "thrift.worker-executor-key", "my-executor"
        );

        Injector injector =
                bootstrap.doNotInitializeLogging()
                         .strictConfig()
                         .setRequiredConfigurationProperties(properties)
                         .initialize();

        ThriftServer server = injector.getInstance(ThriftServer.class);
        assertEquals(server.getWorkerExecutor(), myExecutor);
    }

    /**
     * Creates a {@link com.facebook.swift.service.guice.ThriftServerModule} with the binding
     * for {@link com.facebook.swift.service.ThriftServerConfig} overridden to specify a specific
     * instance of {@link java.util.concurrent.ExecutorService} for the worker executor.
     */
    private Module overrideThriftServerModuleWithWorkerExecutorInstance(final ExecutorService myExecutor)
    {
        return Modules.override(new ThriftServerModule()).with(
                new AbstractModule()
                {
                    @Override
                    protected void configure()
                    {
                        bind(ThriftServerConfig.class).toInstance(
                                new ThriftServerConfig().setWorkerExecutor(myExecutor)
                        );
                    }
                }
        );
    }

    @ThriftService
    private static class ExampleService
    {
        // Intentionally empty: this is just a placeholder
    }
}
