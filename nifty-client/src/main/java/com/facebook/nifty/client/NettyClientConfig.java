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
package com.facebook.nifty.client;

import org.jboss.netty.util.Timer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class NettyClientConfig
{
    private final Map<String, Object> bootstrapOptions;
    private final InetSocketAddress defaultSocksProxyAddress;
    private final Timer timer;
    private final ExecutorService bossExecutor;
    private final int bossThreadCount;
    private final ExecutorService workerExecutor;
    private final int workerThreadCount;

    public NettyClientConfig(Map<String, Object> bootstrapOptions,
                             InetSocketAddress defaultSocksProxyAddress,
                             Timer timer,
                             ExecutorService bossExecutor,
                             int bossThreadCount,
                             ExecutorService workerExecutor,
                             int workerThreadCount)

    {
        this.bootstrapOptions = bootstrapOptions;
        this.defaultSocksProxyAddress = defaultSocksProxyAddress;
        this.timer = timer;
        this.bossExecutor = bossExecutor;
        this.bossThreadCount = bossThreadCount;
        this.workerExecutor = workerExecutor;
        this.workerThreadCount = workerThreadCount;
    }

    public Map<String, Object> getBootstrapOptions()
    {
        return bootstrapOptions;
    }

    public ExecutorService getBossExecutor()
    {
        return bossExecutor;
    }

    public int getBossThreadCount()
    {
        return bossThreadCount;
    }

    public InetSocketAddress getDefaultSocksProxyAddress()
    {
        return defaultSocksProxyAddress;
    }

    public Timer getTimer()
    {
        return timer;
    }

    public ExecutorService getWorkerExecutor()
    {
        return workerExecutor;
    }

    public int getWorkerThreadCount()
    {
        return workerThreadCount;
    }

    public static NettyClientConfigBuilder newBuilder()
    {
        return new NettyClientConfigBuilder();
    }
}
