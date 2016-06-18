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
package com.facebook.swift.service.guice;

import com.facebook.nifty.client.NettyClientConfig;
import com.facebook.nifty.client.NettyClientConfigBuilder;
import com.facebook.nifty.client.NiftyClient;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftClientManagerConfig;
import com.google.inject.*;

import static com.facebook.swift.service.guice.ClientEventHandlersBinder.clientEventHandlersBinder;
import static io.airlift.configuration.ConfigBinder.configBinder;

public class ThriftClientModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        configBinder(binder).bindConfig(ThriftClientManagerConfig.class);

        binder.bind(NiftyClient.class).toProvider(NiftyClientProvider.class).in(Scopes.SINGLETON);

        // Bind single shared ThriftClientManager
        binder.bind(ThriftClientManager.class).in(Scopes.SINGLETON);

        // Create a multibinder for global event handlers
        clientEventHandlersBinder(binder);
    }

    private static class NiftyClientProvider implements Provider<NiftyClient>
    {
        private final ThriftClientManagerConfig clientManagerConfig;

        @Inject
        public NiftyClientProvider(ThriftClientManagerConfig clientManagerConfig)
        {
            this.clientManagerConfig = clientManagerConfig;
        }

        @Override
        public NiftyClient get()
        {
            NettyClientConfigBuilder builder = NettyClientConfig.newBuilder();

            builder.setDefaultSocksProxyAddress(clientManagerConfig.getDefaultSocksProxyAddress());

            if (clientManagerConfig.getWorkerThreadCount() != null) {
                builder.setWorkerThreadCount(clientManagerConfig.getWorkerThreadCount());
            }

            if (clientManagerConfig.getSslClientConfiguration() != null) {
                builder.setSSLClientConfiguration(clientManagerConfig.getSslClientConfiguration());
            }

            return new NiftyClient(builder.build());
        }
    }
}
