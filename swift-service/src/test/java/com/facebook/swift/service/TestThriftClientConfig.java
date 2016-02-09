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
import com.google.common.net.HostAndPort;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.airlift.bootstrap.Bootstrap;
import io.airlift.configuration.testing.ConfigAssertions;
import io.airlift.units.Duration;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

import static io.airlift.configuration.ConfigBinder.configBinder;

public class TestThriftClientConfig
{
    @Test
    public void testDefaults()
    {
        ConfigAssertions.assertRecordedDefaults(ConfigAssertions.recordDefaults(ThriftClientConfig.class)
                                                .setConnectTimeout(Duration.valueOf("500ms"))
                                                .setReceiveTimeout(Duration.valueOf("1m"))
                                                .setReadTimeout(Duration.valueOf("10s"))
                                                .setWriteTimeout(Duration.valueOf("1m"))
                                                .setSocksProxy(null)
                                                .setMaxFrameSize(16777216));
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
            .put("thrift.client.connect-timeout", "10s")
            .put("thrift.client.receive-timeout", "1d")
            .put("thrift.client.read-timeout", "10h")
            .put("thrift.client.write-timeout", "1s")
            .put("thrift.client.socks-proxy", "localhost:8080")
            .put("thrift.client.max-frame-size", "200")
            .build();

        ThriftClientConfig expected = new ThriftClientConfig()
            .setConnectTimeout(Duration.valueOf("10s"))
            .setReceiveTimeout(Duration.valueOf("1d"))
            .setReadTimeout(Duration.valueOf("10h"))
            .setWriteTimeout(Duration.valueOf("1s"))
            .setSocksProxy(HostAndPort.fromParts("localhost", 8080))
            .setMaxFrameSize(200);

        ConfigAssertions.assertFullMapping(properties, expected);
    }

    @Test
    public void testGuiceInjection()
        throws Exception
    {
        Bootstrap bootstrap = new Bootstrap(new Module() {

            @Override
            public void configure(Binder binder)
            {
                configBinder(binder).bindConfig(ThriftClientConfig.class);
            }
        });

        Map<String, String> properties = ImmutableMap.of();

        Injector injector = bootstrap.doNotInitializeLogging()
                        .strictConfig()
                        .setRequiredConfigurationProperties(properties)
                        .initialize();

        ThriftClientConfig clientConfig = injector.getInstance(ThriftClientConfig.class);
        Assert.assertNotNull(clientConfig);

    }
}
