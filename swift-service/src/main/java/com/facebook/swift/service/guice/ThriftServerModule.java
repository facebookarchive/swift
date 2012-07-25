/**
 * Copyright 2012 Facebook, Inc.
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
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.facebook.swift.service.guice.ThriftServiceExporter.ThriftServiceExport;
import com.facebook.swift.service.guice.ThriftServiceExporter.ThriftServiceProcessorProvider;
import com.google.common.base.Throwables;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import org.apache.thrift.TProcessor;
import org.weakref.jmx.guice.ExportBinder;
import org.weakref.jmx.guice.ObjectNameFunction;

import javax.inject.Singleton;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Map;

import static com.facebook.swift.service.metadata.ThriftServiceMetadata.getThriftServiceAnnotation;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static io.airlift.configuration.ConfigurationModule.bindConfig;
import static java.lang.String.format;

public class ThriftServerModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        newSetBinder(binder, ThriftServiceExport.class).permitDuplicates();
        binder.bind(ThriftServiceProcessor.class).toProvider(ThriftServiceProcessorProvider.class).in(Scopes.SINGLETON);
        binder.bind(TProcessor.class).to(Key.get(ThriftServiceProcessor.class)).in(Scopes.SINGLETON);

        bindConfig(binder).to(ThriftServerConfig.class);
        binder.bind(ThriftServer.class).in(Scopes.SINGLETON);

        // export methods from processors to JMX
        ExportBinder.newExporter(binder).exportMap(ThriftMethodProcessor.class).withGeneratedName(new ObjectNameFunction<ThriftMethodProcessor>() {
            @Override
            public ObjectName name(ThriftMethodProcessor methodProcessor)
            {
                try {
                    Class<?> serviceClass = methodProcessor.getServiceClass();
                    String name = format("com.facebook.swift.server:type=%s,name=%s",
                            getServiceName(serviceClass),
                            methodProcessor.getName());

                    return new ObjectName(name);
                }
                catch (MalformedObjectNameException e) {
                    throw Throwables.propagate(e);
                }
            }
        });
    }

    @Provides
    @Singleton
    public Map<String, ThriftMethodProcessor> getMethodProcessors(ThriftServiceProcessor thriftServiceProcessor)
    {
        // extract method handles into a map so they can be exported individually to JMX
        return thriftServiceProcessor.getMethods();
    }

    private static String getServiceName(Class<?> serviceClass)
    {
        String serviceName = getThriftServiceAnnotation(serviceClass).value();
        if (!serviceName.isEmpty()) {
            return serviceName;
        }
        return serviceClass.getSimpleName();
    }
}
