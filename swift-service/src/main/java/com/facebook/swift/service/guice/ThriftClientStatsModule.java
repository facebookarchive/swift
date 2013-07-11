/*
 * Copyright (C) 2013 Facebook, Inc.
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

import com.facebook.swift.service.*;
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
import java.util.concurrent.ConcurrentMap;

import static com.facebook.swift.service.guice.ClientEventHandlersBinder.clientEventHandlersBinder;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static java.lang.String.format;

public class ThriftClientStatsModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        // We bind the ThriftClientProviderProviders in a Set so below we can export the thrift methods to JMX
        newSetBinder(binder, ThriftClientBinder.ThriftClientProvider.class).permitDuplicates();
        binder.bind(ThriftClientStatsHandler.class).in(Scopes.SINGLETON);
        clientEventHandlersBinder(binder).addHandler(ThriftClientStatsHandler.class);
        ExportBinder.newExporter(binder)
                .exportMap(ObjectName.class, ThriftMethodStats.class)
                .withGeneratedName(new MapObjectNameFunction<ObjectName, ThriftMethodStats>()
                {
                    @Override
                    public ObjectName name(ObjectName key, ThriftMethodStats value)
                    {
                        return key;
                    }
                });
    }

    @Provides
    @Singleton
    public Map<ObjectName, ThriftMethodStats> getClientStats(Set<ThriftClientBinder.ThriftClientProvider> clientProviders,
                                                             Set<ThriftClientEventHandler> eventHandlers)
    {
        // find a ThriftClientStatsHandler in eventHandlers
        ConcurrentMap<String, ThriftMethodStats> stats = null;
        for (ThriftClientEventHandler h: eventHandlers) {
            if (h instanceof ThriftClientStatsHandler) {
                stats = ((ThriftClientStatsHandler)h).getStats();
                break;
            }
        }
        if (stats == null) {
            return ImmutableMap.of();
        }
        try {
            // extract method handlers into a map so they can be exported individually into jmx
            ImmutableMap.Builder<ObjectName, ThriftMethodStats> builder = ImmutableMap.builder();
            for (ThriftClientBinder.ThriftClientProvider<?> clientProvider : clientProviders) {
                ThriftClientManager.ThriftClientMetadata clientMetadata = clientProvider.getClientMetadata();
                for (ThriftMethodHandler methodHandler : clientMetadata.getMethodHandlers().values()) {
                    String name = format("com.facebook.swift.client:type=%s,clientName=%s,name=%s",
                            clientMetadata.getClientType(),
                            clientMetadata.getClientName(),
                            methodHandler.getName());
                    stats.putIfAbsent(methodHandler.getQualifiedName(), new ThriftMethodStats());
                    builder.put(ObjectName.getInstance(name), stats.get(methodHandler.getQualifiedName()));
                }
            }
            return builder.build();
        }
        catch (MalformedObjectNameException e) {
            throw Throwables.propagate(e);
        }
    }
}
