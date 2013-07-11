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
import com.facebook.swift.service.ThriftClientEventHandler;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftClientManager.ThriftClientMetadata;
import com.facebook.swift.service.ThriftMethodHandler;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.inject.*;
import org.weakref.jmx.guice.ExportBinder;
import org.weakref.jmx.guice.MapObjectNameFunction;

import javax.inject.Singleton;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

import static com.facebook.swift.service.guice.ClientEventHandlersBinder.clientEventHandlersBinder;
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

        // Create a multibinder for global event handlers
        clientEventHandlersBinder(binder);
    }
}
