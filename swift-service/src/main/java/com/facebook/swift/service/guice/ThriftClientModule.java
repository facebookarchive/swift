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

import com.facebook.nifty.client.NiftyClient;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftClientManager.ThriftClientMetadata;
import com.facebook.swift.service.ThriftMethodHandler;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import org.weakref.jmx.guice.ExportBinder;
import org.weakref.jmx.guice.MapObjectNameFunction;

import javax.inject.Singleton;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static io.airlift.configuration.ConfigurationModule.bindConfig;
import static java.lang.String.format;

public class ThriftClientModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(NiftyClient.class).in(Scopes.SINGLETON);

        // Bind single shared ThriftClientManager
        binder.bind(ThriftClientManager.class).in(Scopes.SINGLETON);

        // We bind the ThriftClientProviderProviders in a Set so below we can export the thrift methods to JMX
        newSetBinder(binder, ThriftClientBinder.ThriftClientProvider.class).permitDuplicates();
        ExportBinder.newExporter(binder)
                .exportMap(ObjectName.class, ThriftMethodHandler.class)
                .withGeneratedName(new MapObjectNameFunction<ObjectName, ThriftMethodHandler>()
                {
                    @Override
                    public ObjectName name(ObjectName key, ThriftMethodHandler value)
                    {
                        return key;
                    }
                });
    }

    @Provides
    @Singleton
    public Map<ObjectName, ThriftMethodHandler> getMethodProcessors(Set<ThriftClientBinder.ThriftClientProvider> clientProviders)
    {
        try {
            // extract method handles into a map so they can be exported individually into jmx
            ImmutableMap.Builder<ObjectName, ThriftMethodHandler> builder = ImmutableMap.builder();
            for (ThriftClientBinder.ThriftClientProvider<?> clientProvider : clientProviders) {
                ThriftClientMetadata clientMetadata = clientProvider.getClientMetadata();
                for (ThriftMethodHandler methodHandler : clientMetadata.getMethodHandlers().values()) {
                    String name = format("com.facebook.swift.client:type=%s,clientName=%s,name=%s",
                            clientMetadata.getClientType(),
                            clientMetadata.getClientName(),
                            methodHandler.getName());
                    builder.put(ObjectName.getInstance(name), methodHandler);
                }
            }
            return builder.build();
        }
        catch (MalformedObjectNameException e) {
            throw Throwables.propagate(e);
        }
    }
}
