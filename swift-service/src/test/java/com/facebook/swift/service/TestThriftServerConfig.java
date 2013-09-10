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

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.configuration.testing.ConfigAssertions;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import org.testng.annotations.Test;

import java.util.Map;

import static io.airlift.configuration.ConfigurationModule.bindConfig;
import static org.testng.Assert.assertNotNull;

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
                .put("thrift.transport", "buffered")
                .put("thrift.protocol", "compact")
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
                .setTransportName("buffered")
                .setProtocolName("compact");

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
                bindConfig(binder()).to(ThriftServerConfig.class);
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
}
