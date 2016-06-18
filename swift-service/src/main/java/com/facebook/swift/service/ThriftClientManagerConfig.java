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

import com.facebook.nifty.ssl.SslClientConfiguration;
import com.google.common.net.HostAndPort;
import io.airlift.configuration.Config;

public class ThriftClientManagerConfig
{
    private HostAndPort defaultSocksProxyAddress = null;
    private Integer workerThreadCount = null;
    private SslClientConfiguration sslClientConfiguration = null;

    public HostAndPort getDefaultSocksProxyAddress()
    {
        return defaultSocksProxyAddress;
    }

    public Integer getWorkerThreadCount() { return workerThreadCount; }

    public SslClientConfiguration getSslClientConfiguration() { return sslClientConfiguration; }

    @Config("thrift.clientmanager.default-socks-proxy")
    public void setDefaultSocksProxyAddress(HostAndPort defaultSocksProxyAddress)
    {
        this.defaultSocksProxyAddress = defaultSocksProxyAddress;
    }

    @Config("thrift.clientmanager.worker-thread-count")
    public void setWorkerThreadCount(int workerThreadCount)
    {
        this.workerThreadCount = Integer.valueOf(workerThreadCount);
    }

    public void setSslClientConfiguration(SslClientConfiguration sslClientConfiguration) {
        this.sslClientConfiguration = sslClientConfiguration;
    }
}
