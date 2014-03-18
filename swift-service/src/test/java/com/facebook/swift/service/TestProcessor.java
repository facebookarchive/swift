/*
 * Copyright (C) 2014 Facebook, Inc.
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

import com.facebook.swift.codec.guice.ThriftCodecModule;
import com.facebook.swift.service.guice.ThriftServerModule;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import io.airlift.configuration.ConfigurationFactory;
import io.airlift.configuration.ConfigurationModule;
import org.testng.annotations.Test;

import static com.facebook.swift.service.guice.ThriftServiceExporter.thriftServerBinder;

public class TestProcessor
{
    /**
     * Check that defining multiple {@link com.facebook.swift.service.ThriftMethod}-annotated
     * methods with the same name triggers an error when creating the
     * {@link com.facebook.swift.service.ThriftServiceProcessor}
     */
    @Test(expectedExceptions = Exception.class)
    public void testThriftServiceProcessor()
    {
        Injector injector = Guice.createInjector(
                Stage.DEVELOPMENT,
                new ConfigurationModule(new ConfigurationFactory(ImmutableMap.<String, String>of())),
                new ThriftCodecModule(),
                new ThriftServerModule(),
                new AbstractModule()
                {
                    @Override
                    protected void configure()
                    {
                        bind(DoSomethingServiceOne.class);
                        bind(DoSomethingServiceTwo.class);

                        thriftServerBinder(binder()).exportThriftService(DoSomethingServiceOne.class);
                        thriftServerBinder(binder()).exportThriftService(DoSomethingServiceTwo.class);
                    }
                }
        );

        injector.getInstance(ThriftServiceProcessor.class);
    }

    @ThriftService
    public static final class DoSomethingServiceOne {
        @ThriftMethod
        public void doSomething() {}
    }

    @ThriftService
    public static final class DoSomethingServiceTwo {
        @ThriftMethod
        public void doSomething() {}
    }
}
