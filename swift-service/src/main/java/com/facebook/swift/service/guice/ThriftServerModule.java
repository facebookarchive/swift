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

import com.facebook.nifty.codec.DefaultThriftFrameCodecFactory;
import com.facebook.nifty.codec.ThriftFrameCodecFactory;
import com.facebook.nifty.core.NiftyTimer;
import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.nifty.ssl.SslServerConfiguration;
import com.facebook.nifty.ssl.TransportAttachObserver;
import com.facebook.swift.service.ThriftEventHandler;
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.ThriftServerTimer;
import com.facebook.swift.service.ThriftServerWorkerExecutor;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.facebook.swift.service.guice.ThriftServiceExporter.ThriftServiceExport;
import com.facebook.swift.service.guice.ThriftServiceExporter.ThriftServiceProcessorProvider;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.binder.ScopedBindingBuilder;

import com.google.inject.multibindings.MapBinder;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.jboss.netty.util.Timer;

import java.util.concurrent.ExecutorService;

import static com.google.inject.multibindings.MapBinder.newMapBinder;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static io.airlift.configuration.ConfigBinder.configBinder;

public class ThriftServerModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        // Setup map binder for executor services
        workerExecutorBinder(binder).permitDuplicates();

        // Setup map binder for message frame codecs
        newMapBinder(binder, String.class, ThriftFrameCodecFactory.class).permitDuplicates();

        // ...and bind unframed (aka buffered) and framed codecs by default. The default frame codec
        // factory from Nifty handles both equally well.
        bindFrameCodecFactory(binder, "unframed", DefaultThriftFrameCodecFactory.class);
        bindFrameCodecFactory(binder, "buffered", DefaultThriftFrameCodecFactory.class);
        bindFrameCodecFactory(binder, "framed", DefaultThriftFrameCodecFactory.class);

        // Setup binder for protocols...
        newMapBinder(binder, String.class, TDuplexProtocolFactory.class).permitDuplicates();

        // ...and bind binary and compact protocols by default
        bindProtocolFactory(binder, "binary", TDuplexProtocolFactory.fromSingleFactory(new TBinaryProtocol.Factory()));
        bindProtocolFactory(binder, "compact", TDuplexProtocolFactory.fromSingleFactory(new TCompactProtocol.Factory()));

        newSetBinder(binder, ThriftServiceExport.class).permitDuplicates();
        newSetBinder(binder, ThriftEventHandler.class).permitDuplicates();
        binder.bind(ThriftServiceProcessor.class).toProvider(ThriftServiceProcessorProvider.class).in(Scopes.SINGLETON);
        binder.bind(NiftyProcessor.class).to(Key.get(ThriftServiceProcessor.class)).in(Scopes.SINGLETON);

        configBinder(binder).bindConfig(ThriftServerConfig.class);
        binder.bind(ThriftServer.NiftySecurityFactoryHolder.class);
        binder.bind(ThriftServer.class).in(Scopes.SINGLETON);
        binder.bind(ThriftServer.SslServerConfigurationHolder.class);
        binder.bind(ThriftServer.TransportAttachObserverHolder.class);
    }

    // helpers for binding frame codec factories

    public static ScopedBindingBuilder bindFrameCodecFactory(Binder binder, String key, Class<? extends ThriftFrameCodecFactory> frameCodecFactoryClass)
    {
        return newMapBinder(binder, String.class, ThriftFrameCodecFactory.class).addBinding(key).to(frameCodecFactoryClass);
    }

    public static void bindFrameCodecFactory(Binder binder, String key, ThriftFrameCodecFactory frameCodecFactory)
    {
        newMapBinder(binder, String.class, ThriftFrameCodecFactory.class).addBinding(key).toInstance(frameCodecFactory);
    }

    // helpers for binding protocol factories

    public static ScopedBindingBuilder bindProtocolFactory(Binder binder, String key, Class<? extends TDuplexProtocolFactory> protocolFactoryClass)
    {
        return newMapBinder(binder, String.class, TDuplexProtocolFactory.class).addBinding(key).to(protocolFactoryClass);
    }

    public static void bindProtocolFactory(Binder binder, String key, TDuplexProtocolFactory protocolFactory)
    {
        newMapBinder(binder, String.class, TDuplexProtocolFactory.class).addBinding(key).toInstance(protocolFactory);
    }

    // Helpers for binding worker executors

    public static ScopedBindingBuilder bindWorkerExecutor(Binder binder, String key, Class<? extends ExecutorService> executorServiceClass)
    {
        return workerExecutorBinder(binder).addBinding(key).to(executorServiceClass);
    }

    public static ScopedBindingBuilder bindWorkerExecutor(Binder binder, String key, Provider<? extends ExecutorService> executorServiceProvider)
    {
        return workerExecutorBinder(binder).addBinding(key).toProvider(executorServiceProvider);
    }

    public static ScopedBindingBuilder bindWorkerExecutorProvider(Binder binder, String key, Class<? extends javax.inject.Provider<? extends ExecutorService>> executorServiceProvider)
    {
      return workerExecutorBinder(binder).addBinding(key).toProvider(executorServiceProvider);
    }

    public static void bindWorkerExecutor(Binder binder, String key, ExecutorService executorService)
    {
        workerExecutorBinder(binder).addBinding(key).toInstance(executorService);
    }

    private static MapBinder<String, ExecutorService> workerExecutorBinder(Binder binder)
    {
        return newMapBinder(binder, String.class, ExecutorService.class, ThriftServerWorkerExecutor.class);
    }

    @Provides
    @ThriftServerTimer
    @Singleton
    public Timer getThriftServerTimer()
    {
        return new NiftyTimer("thrift");
    }
}
