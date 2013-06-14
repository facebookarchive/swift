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

import com.facebook.swift.service.ThriftMethodProcessor;
import com.facebook.swift.service.ThriftMethodStats;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.facebook.swift.service.ThriftServiceStatsHandler;
import com.google.common.base.Throwables;
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
import java.util.concurrent.ConcurrentMap;

import static com.facebook.swift.service.guice.ThriftServiceExporter.thriftServerBinder;
import static java.lang.String.format;

public class ThriftServerStatsModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(ThriftServiceStatsHandler.class).in(Scopes.SINGLETON);
        thriftServerBinder(binder).addEventHandler(ThriftServiceStatsHandler.class);

        ExportBinder.newExporter(binder).exportMap(ThriftMethodStats.class).withGeneratedName(
                new MapObjectNameFunction<String, ThriftMethodStats>()
                {
                    @Override
                    public ObjectName name(String methodName, ThriftMethodStats methodStats)
                    {
                        try {
                            int dot = methodName.indexOf('.');
                            String serviceName = methodName.substring(0, dot);
                            methodName = methodName.substring(dot + 1);
                            String name = format("com.facebook.swift.server:type=%s,name=%s",
                                    serviceName,
                                    methodName);

                            return new ObjectName(name);
                        } catch (MalformedObjectNameException e) {
                            throw Throwables.propagate(e);
                        }
                    }
                });
    }

    @Provides
    @Singleton
    public Map<String, ThriftMethodStats> getMethodStats(ThriftServiceProcessor thriftServiceProcessor,
                                                         ThriftServiceStatsHandler serviceStatsHandler)
    {
        // populate stats map with method names mapped to empty stat objects and tell JMX to export that
        final ConcurrentMap<String, ThriftMethodStats> stats = serviceStatsHandler.getStats();
        for (Map.Entry<String, ThriftMethodProcessor> entry: thriftServiceProcessor.getMethods().entrySet()) {
            stats.putIfAbsent(entry.getValue().getQualifiedName(), new ThriftMethodStats());
        }
        return stats;
    }
}
