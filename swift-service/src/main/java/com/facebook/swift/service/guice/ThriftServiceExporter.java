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

import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftEventHandler;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.Set;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

public class ThriftServiceExporter
{
    public static ThriftServiceExporter thriftServerBinder(Binder binder)
    {
        return new ThriftServiceExporter(binder);
    }

    private final Binder binder;

    private ThriftServiceExporter(Binder binder)
    {
        this.binder = binder;
    }

    public <T> void exportThriftService(Class<T> thriftServer)
    {
        Preconditions.checkNotNull(thriftServer, "thriftServer is null");
        newSetBinder(binder, ThriftServiceExport.class).addBinding().toInstance(new ThriftServiceExport(Key.get(thriftServer)));
    }

    public <T> void exportThriftService(Class<T> thriftServer, Class<? extends Annotation> annotationType)
    {
        Preconditions.checkNotNull(thriftServer, "thriftServer is null");
        newSetBinder(binder, ThriftServiceExport.class).addBinding().toInstance(new ThriftServiceExport(Key.get(thriftServer, annotationType)));
    }

    public <T> void exportThriftService(Key<T> key)
    {
        Preconditions.checkNotNull(key, "key is null");
        newSetBinder(binder, ThriftServiceExport.class).addBinding().toInstance(new ThriftServiceExport(key));
    }

    public void addEventHandler(ThriftEventHandler handler)
    {
        Preconditions.checkNotNull(handler, "handler is null");
        newSetBinder(binder, ThriftEventHandler.class).addBinding().toInstance(handler);
    }

    public void addEventHandler(Key<? extends ThriftEventHandler> key)
    {
        Preconditions.checkNotNull(key, "key is null");
        newSetBinder(binder, ThriftEventHandler.class).addBinding().to(key);
    }

    public void addEventHandler(Class<? extends ThriftEventHandler> cls)
    {
        Preconditions.checkNotNull(cls, "cls is null");
        newSetBinder(binder, ThriftEventHandler.class).addBinding().to(cls);
    }

    public static class ThriftServiceProcessorProvider implements Provider<ThriftServiceProcessor>
    {
        private final Injector injector;
        private final ThriftCodecManager codecManager;
        private final Set<ThriftEventHandler> eventHandlers;
        private final Set<ThriftServiceExport> serviceExports;

        @Inject
        public ThriftServiceProcessorProvider(Injector injector, ThriftCodecManager codecManager,
                                              Set<ThriftEventHandler> eventHandlers, Set<ThriftServiceExport> serviceExports)
        {
            this.injector = injector;
            this.codecManager = codecManager;
            this.eventHandlers = eventHandlers;
            this.serviceExports = serviceExports;
        }

        @Override
        public ThriftServiceProcessor get()
        {
            ImmutableList.Builder<Object> servers = ImmutableList.builder();
            for (ThriftServiceExport serviceExport : serviceExports) {
                Object server = injector.getInstance(serviceExport.getKey());
                servers.add(server);
            }
            return new ThriftServiceProcessor(codecManager, ImmutableList.copyOf(eventHandlers), servers.build());
        }
    }

    public static class ThriftServiceExport
    {
        private final Key<?> key;

        public ThriftServiceExport(Key<?> key)
        {
            this.key = key;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ThriftServiceExport that = (ThriftServiceExport) o;

            if (!key.equals(that.key)) {
                return false;
            }

            return true;
        }

        public Key<?> getKey()
        {
            return key;
        }

        @Override
        public int hashCode()
        {
            return key.hashCode();
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("ThriftServerBinding");
            sb.append("{key=").append(key);
            sb.append('}');
            return sb.toString();
        }
    }
}
