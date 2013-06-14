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

import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.swift.service.*;
import com.facebook.swift.service.guice.ThriftServiceExporter.ThriftServiceExport;
import com.facebook.swift.service.guice.ThriftServiceExporter.ThriftServiceProcessorProvider;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scopes;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static io.airlift.configuration.ConfigurationModule.bindConfig;

public class ThriftServerModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(Timer.class).annotatedWith(ThriftServerTimer.class).toInstance(new HashedWheelTimer());

        newSetBinder(binder, ThriftServiceExport.class).permitDuplicates();
        newSetBinder(binder, ThriftEventHandler.class).permitDuplicates();
        binder.bind(ThriftServiceProcessor.class).toProvider(ThriftServiceProcessorProvider.class).in(Scopes.SINGLETON);
        binder.bind(NiftyProcessor.class).to(Key.get(ThriftServiceProcessor.class)).in(Scopes.SINGLETON);

        bindConfig(binder).to(ThriftServerConfig.class);
        binder.bind(ThriftServer.class).in(Scopes.SINGLETON);
    }
}
