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

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.nifty.client.NiftyClientConnector;
import com.facebook.swift.codec.guice.ThriftCodecModule;
import com.facebook.swift.service.LogEntry;
import com.facebook.swift.service.ResultCode;
import com.facebook.swift.service.Scribe;
import com.facebook.swift.service.SwiftScribe;
import com.facebook.swift.service.ThriftClient;
import com.facebook.swift.service.ThriftClientConfig;
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.puma.TestPuma;
import com.facebook.swift.service.puma.swift.PumaReadServer;
import com.facebook.swift.service.puma.swift.PumaReadService;
import com.facebook.swift.service.puma.swift.ReadResultQueryInfoTimeString;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.inject.Binder;
import com.google.inject.BindingAnnotation;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import io.airlift.configuration.ConfigurationFactory;
import io.airlift.configuration.ConfigurationModule;
import io.airlift.jmx.JmxModule;
import io.airlift.units.Duration;
import org.testng.annotations.Test;
import org.weakref.jmx.guice.MBeanModule;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.facebook.swift.service.guice.ThriftClientBinder.thriftClientBinder;
import static com.facebook.swift.service.guice.ThriftServiceExporter.thriftServerBinder;
import static com.facebook.swift.service.puma.TestPuma.verifyPumaResults;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.testng.Assert.assertEquals;

public class TestThriftClientAndServerModules
{
    private static final List<LogEntry> MESSAGES = ImmutableList.of(
            new LogEntry("hello", "world"),
            new LogEntry("bye", "world")
    );

    @Test
    public void testThriftClientAndServerModules()
            throws Exception
    {
        Injector injector = Guice.createInjector(Stage.PRODUCTION,
                new ConfigurationModule(new ConfigurationFactory(ImmutableMap.<String, String>of())),
                new ThriftCodecModule(),
                new ThriftClientModule(),
                new ThriftServerModule(),
                new JmxModule(),
                new MBeanModule(),
                new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        // bind scribe client
                        thriftClientBinder(binder).bindThriftClient(Scribe.class);
                        // bind scribe service implementation
                        binder.bind(SwiftScribe.class).in(Scopes.SINGLETON);
                        // export scribe service implementation
                        thriftServerBinder(binder).exportThriftService(SwiftScribe.class);

                        // bind puma client
                        thriftClientBinder(binder).bindThriftClient(PumaReadService.class);
                        // bind puma service implementation
                        binder.bind(PumaReadServer.class).in(Scopes.SINGLETON);
                        // export puma service implementation
                        thriftServerBinder(binder).exportThriftService(PumaReadServer.class);
                    }
                });

        try (ThriftServer server = injector.getInstance(ThriftServer.class).start()) {

            // test scribe
            ThriftClient<Scribe> scribeClient = injector.getInstance(Key.get(new TypeLiteral<ThriftClient<Scribe>>() {}));
            try (Scribe scribe = scribeClient.open(localFramedConnector(server.getPort())).get()) {
                assertEquals(scribe.log(MESSAGES), ResultCode.OK);
                assertEquals(injector.getInstance(SwiftScribe.class).getMessages(), newArrayList(MESSAGES));
            }

            // test puma
            ThriftClient<PumaReadService> pumaClient = injector.getInstance(Key.get(new TypeLiteral<ThriftClient<PumaReadService>>() {}));
            try (PumaReadService puma = pumaClient.open(localFramedConnector(server.getPort())).get()) {
                List<ReadResultQueryInfoTimeString> results = puma.getResultTimeString(TestPuma.PUMA_REQUEST);
                verifyPumaResults(results);
            }
        }
    }

    @Test
    public void testThriftWithAnnotationBinding()
            throws Exception
    {
        Injector injector = Guice.createInjector(Stage.PRODUCTION,
                new ConfigurationModule(new ConfigurationFactory(ImmutableMap.<String, String>of())),
                new ThriftCodecModule(),
                new ThriftClientModule(),
                new ThriftServerModule(),
                new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        // bind scribe client
                        thriftClientBinder(binder).bindThriftClient(Scribe.class, TestAnnotation.class);
                        // bind scribe service implementation
                        binder.bind(SwiftScribe.class).annotatedWith(TestAnnotation.class).to(SwiftScribe.class).in(Scopes.SINGLETON);
                        // export scribe service implementation
                        thriftServerBinder(binder).exportThriftService(SwiftScribe.class, TestAnnotation.class);

                        // bind puma client
                        thriftClientBinder(binder).bindThriftClient(PumaReadService.class, TestAnnotation.class);
                        // bind puma service implementation
                        binder.bind(PumaReadServer.class).annotatedWith(TestAnnotation.class).to(PumaReadServer.class).in(Scopes.SINGLETON);
                        // export puma service implementation
                        thriftServerBinder(binder).exportThriftService(PumaReadServer.class, TestAnnotation.class);
                    }
                });

        try (ThriftServer server = injector.getInstance(ThriftServer.class).start()) {
            // test scribe
            ThriftClient<Scribe> scribeClient = injector.getInstance(Key.get(new TypeLiteral<ThriftClient<Scribe>>() {}, TestAnnotation.class));
            try (Scribe scribe = scribeClient.open(localFramedConnector(server.getPort())).get()) {
                assertEquals(scribe.log(MESSAGES), ResultCode.OK);
                assertEquals(injector.getInstance(Key.get(SwiftScribe.class, TestAnnotation.class)).getMessages(), newArrayList(MESSAGES));
            }

            // test puma
            ThriftClient<PumaReadService> pumaClient = injector.getInstance(Key.get(new TypeLiteral<ThriftClient<PumaReadService>>() {}, TestAnnotation.class));
            try (PumaReadService puma = pumaClient.open(localFramedConnector(server.getPort())).get()) {
                List<ReadResultQueryInfoTimeString> results = puma.getResultTimeString(TestPuma.PUMA_REQUEST);
                verifyPumaResults(results);
            }
        }
    }

    @Test
    public void testThriftWithKeyBinding()
            throws Exception
    {
        Injector injector = Guice.createInjector(Stage.PRODUCTION,
                new ConfigurationModule(new ConfigurationFactory(ImmutableMap.<String, String>of())),
                new ThriftCodecModule(),
                new ThriftClientModule(),
                new ThriftServerModule(),
                new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        // bind scribe client
                        thriftClientBinder(binder).bindThriftClient(Scribe.class);
                        // bind scribe service implementation
                        binder.bind(SwiftScribe.class).annotatedWith(TestAnnotation.class).to(SwiftScribe.class).in(Scopes.SINGLETON);
                        // export scribe service implementation
                        thriftServerBinder(binder).exportThriftService(Key.get(SwiftScribe.class, TestAnnotation.class));

                        // bind puma client
                        thriftClientBinder(binder).bindThriftClient(PumaReadService.class);
                        // bind puma service implementation
                        binder.bind(PumaReadServer.class).annotatedWith(TestAnnotation.class).to(PumaReadServer.class).in(Scopes.SINGLETON);
                        // export puma service implementation
                        thriftServerBinder(binder).exportThriftService(Key.get(PumaReadServer.class, TestAnnotation.class));
                    }
                });

        try (ThriftServer server = injector.getInstance(ThriftServer.class).start()) {
            // test scribe
            ThriftClient<Scribe> scribeClient = injector.getInstance(Key.get(new TypeLiteral<ThriftClient<Scribe>>() {}));
            try (Scribe scribe = scribeClient.open(localFramedConnector(server.getPort())).get()) {
                assertEquals(scribe.log(MESSAGES), ResultCode.OK);
                assertEquals(injector.getInstance(Key.get(SwiftScribe.class, TestAnnotation.class)).getMessages(), newArrayList(MESSAGES));
            }

            // test puma
            ThriftClient<PumaReadService> pumaClient = injector.getInstance(Key.get(new TypeLiteral<ThriftClient<PumaReadService>>() {}));
            try (PumaReadService puma = pumaClient.open(localFramedConnector(server.getPort())).get()) {
                List<ReadResultQueryInfoTimeString> results = puma.getResultTimeString(TestPuma.PUMA_REQUEST);
                verifyPumaResults(results);
            }
        }

    }

    @Test
    public void testThriftClientWithConfiguration()
            throws Exception
    {
        ImmutableMap<String, String> configMap = new ImmutableMap.Builder<String, String>()
                .put("scribe.thrift.client.connect-timeout", "1s")
                .put("scribe.thrift.client.read-timeout", "750ms")
                .put("PumaReadService.thrift.client.write-timeout", "10s")
                .build();

        Injector injector = Guice.createInjector(
                Stage.PRODUCTION,
                new ConfigurationModule(new ConfigurationFactory(configMap)),
                new ThriftCodecModule(),
                new ThriftClientModule(),
                new Module() {
                    @Override
                    public void configure(Binder binder)
                    {
                        thriftClientBinder(binder).bindThriftClient(Scribe.class);
                        thriftClientBinder(binder).bindThriftClient(PumaReadService.class);
                    }
                });

        ThriftClient<Scribe> scribeClient = injector.getInstance(Key.get(new TypeLiteral<ThriftClient<Scribe>>() {}));
        assertEquals(Duration.valueOf(scribeClient.getConnectTimeout()), new Duration(1, TimeUnit.SECONDS));
        assertEquals(Duration.valueOf(scribeClient.getReadTimeout()), new Duration(750, TimeUnit.MILLISECONDS));
        assertEquals(Duration.valueOf(scribeClient.getWriteTimeout()), ThriftClientConfig.DEFAULT_WRITE_TIMEOUT);

        ThriftClient<PumaReadService> pumaClient = injector.getInstance(Key.get(new TypeLiteral<ThriftClient<PumaReadService>>() {}));
        assertEquals(Duration.valueOf(pumaClient.getConnectTimeout()), ThriftClientConfig.DEFAULT_CONNECT_TIMEOUT);
        assertEquals(Duration.valueOf(pumaClient.getReadTimeout()), ThriftClientConfig.DEFAULT_READ_TIMEOUT);
        assertEquals(Duration.valueOf(pumaClient.getWriteTimeout()), new Duration(10, TimeUnit.SECONDS));
    }

    private NiftyClientConnector<? extends NiftyClientChannel> localFramedConnector(int port) {
        return new FramedClientConnector(HostAndPort.fromParts("localhost", port));
    }

    @Target({METHOD, CONSTRUCTOR, FIELD, PARAMETER})
    @Retention(RUNTIME)
    @BindingAnnotation
    public @interface TestAnnotation {
    }
}
