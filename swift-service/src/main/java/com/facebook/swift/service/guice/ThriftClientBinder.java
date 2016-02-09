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

import com.facebook.swift.service.ThriftClient;
import com.facebook.swift.service.ThriftClientConfig;
import com.facebook.swift.service.ThriftClientEventHandler;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftClientManager.ThriftClientMetadata;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.weakref.jmx.guice.ExportBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.UUID;

import static com.facebook.swift.service.ThriftClientManager.DEFAULT_NAME;
import static com.facebook.swift.service.metadata.ThriftServiceMetadata.getThriftServiceAnnotation;
import static io.airlift.configuration.ConfigBinder.configBinder;
import static java.lang.String.format;

public class ThriftClientBinder
{
    public static ThriftClientBinder thriftClientBinder(Binder binder)
    {
        return new ThriftClientBinder(binder);
    }

    private final Binder binder;

    private ThriftClientBinder(Binder binder)
    {
        this.binder = binder;
    }

    public <T> ClientEventHandlersBinder bindThriftClient(Class<T> clientInterface)
    {
        Preconditions.checkNotNull(clientInterface, "clientInterface is null");
        String typeName = getServiceName(clientInterface);

        // Bind ThriftClientConfig with random @Named annotation
        // We generate a random Named annotation for binding the ThriftClientConfig because we
        // need one of these for each clientType+annotationType pair.  Without this, the
        // ThriftClientConfig bindings collapse to a single instance which is shared by all
        // clients.
        Named thriftClientConfigKey = Names.named(typeName + "-" + UUID.randomUUID().toString());
        configBinder(binder).bindConfig(ThriftClientConfig.class, thriftClientConfigKey, typeName);

        // Bind ThriftClient to a provider which knows how to find the ThriftClientConfig using
        // the random @Named annotation
        Multibinder<ThriftClientEventHandler> eventHandlersBinder = Multibinder.newSetBinder(binder,
                ThriftClientEventHandler.class, thriftClientConfigKey);
        ThriftClientProvider<T> provider = new ThriftClientProvider<>(clientInterface, DEFAULT_NAME,
                Key.get(ThriftClientConfig.class, thriftClientConfigKey),
                Key.get(new TypeLiteral<Set<ThriftClientEventHandler>>() {}, thriftClientConfigKey));
        TypeLiteral<ThriftClient<T>> typeLiteral = toThriftClientTypeLiteral(clientInterface);
        binder.bind(typeLiteral).toProvider(provider).in(Scopes.SINGLETON);

        // Export client to jmx
        ExportBinder.newExporter(binder)
                .export(Key.get(typeLiteral))
                .as(format("com.facebook.swift.client:type=%s,clientName=%s",
                        typeName,
                        DEFAULT_NAME));

        // Add the provider itself to a SetBinding so later we can export the thrift methods to JMX
        Multibinder.newSetBinder(binder, ThriftClientProvider.class).addBinding().toInstance(provider);

        return new ClientEventHandlersBinder(eventHandlersBinder);
    }

    public <T> ClientEventHandlersBinder bindThriftClient(Class<T> clientInterface, Class<? extends Annotation> annotationType)
    {
        Preconditions.checkNotNull(clientInterface, "clientInterface is null");
        String typeName = getServiceName(clientInterface);
        String name = annotationType.getSimpleName();

        // Bind ThriftClientConfig with random @Named annotation
        // see comment on random Named annotation above
        Named thriftClientConfigKey = Names.named(typeName + "-" + UUID.randomUUID().toString());
        String prefix = String.format("%s.%s", typeName, name);
        configBinder(binder).bindConfig(ThriftClientConfig.class, thriftClientConfigKey, prefix);

        // Bind ThriftClient to a provider which knows how to find the ThriftClientConfig using
        // the random @Named annotation
        Multibinder<ThriftClientEventHandler> eventHandlersBinder = Multibinder.newSetBinder(binder,
                ThriftClientEventHandler.class, thriftClientConfigKey);
        ThriftClientProvider<T> provider = new ThriftClientProvider<>(clientInterface,
                name,
                Key.get(ThriftClientConfig.class, thriftClientConfigKey),
                Key.get(new TypeLiteral<Set<ThriftClientEventHandler>>() {}, thriftClientConfigKey));
        TypeLiteral<ThriftClient<T>> typeLiteral = toThriftClientTypeLiteral(clientInterface);
        binder.bind(Key.get(typeLiteral, annotationType)).toProvider(provider).in(Scopes.SINGLETON);

        // Export client to jmx
        ExportBinder.newExporter(binder)
                .export(Key.get(typeLiteral))
                .as(format("com.facebook.swift.client:type=%s,clientName=%s",
                           typeName,
                           name));

        // Add the provider itself to a SetBinding so later we can export the thrift methods to JMX
        Multibinder.newSetBinder(binder, ThriftClientProvider.class).addBinding().toInstance(provider);

        return new ClientEventHandlersBinder(eventHandlersBinder);
    }

    private static String getServiceName(Class<?> clientInterface)
    {
        String serviceName = getThriftServiceAnnotation(clientInterface).value();
        if (!serviceName.isEmpty()) {
            return serviceName;
        }
        return clientInterface.getSimpleName();
    }

    /**
     * @return TypeLiteral<ThriftClient<T>>
     */
    private static <T> TypeLiteral<ThriftClient<T>> toThriftClientTypeLiteral(Class<T> clientInterface)
    {
        // build a TypeLiteral<ThriftClientProvider<T>> where T is bound to the actual clientInterface class
        @SuppressWarnings("serial")
        Type javaType = new TypeToken<ThriftClient<T>>() {}
                .where(new TypeParameter<T>() {}, TypeToken.of(clientInterface))
                .getType();
        return (TypeLiteral<ThriftClient<T>>) TypeLiteral.get(javaType);
    }

    public static class ThriftClientProvider<T> implements Provider<ThriftClient<T>>
    {
        private final Class<T> clientType;
        private final String clientName;
        private final Key<ThriftClientConfig> configKey;
        private final Key<Set<ThriftClientEventHandler>> eventHandlersKey;
        private ThriftClientManager clientManager;
        private Injector injector;

        public ThriftClientProvider(Class<T> clientType, String clientName, Key<ThriftClientConfig> configKey,
                                    Key<Set<ThriftClientEventHandler>> eventHandlersKey)
        {
            Preconditions.checkNotNull(clientType, "clientInterface is null");
            Preconditions.checkNotNull(clientName, "clientName is null");
            Preconditions.checkNotNull(configKey, "configKey is null");
            Preconditions.checkNotNull(eventHandlersKey, "eventHandlersKey is null");
            this.eventHandlersKey = eventHandlersKey;
            this.clientType = clientType;
            this.clientName = clientName;
            this.configKey = configKey;
        }

        @Inject
        public void setClientManager(ThriftClientManager clientManager)
        {
            this.clientManager = clientManager;
        }

        @Inject
        public void setInjector(Injector injector)
        {
            this.injector = injector;
        }

        @Override
        public ThriftClient<T> get()
        {
            Preconditions.checkState(clientManager != null, "clientManager has not been set");
            Preconditions.checkState(injector != null, "injector has not been set");
            ThriftClientConfig clientConfig = injector.getInstance(configKey);
            Set<ThriftClientEventHandler> handlersSet = injector.getInstance(eventHandlersKey);
            return new ThriftClient<>(clientManager, clientType, clientConfig, clientName, ImmutableList.copyOf(handlersSet));
        }

        public ThriftClientMetadata getClientMetadata()
        {
            Preconditions.checkState(clientManager != null, "clientManager has not been set");
            return clientManager.getClientMetadata(clientType, clientName);
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

            ThriftClientProvider<?> that = (ThriftClientProvider<?>) o;

            if (!clientName.equals(that.clientName)) {
                return false;
            }
            if (!clientType.equals(that.clientType)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = clientType.hashCode();
            result = 31 * result + clientName.hashCode();
            return result;
        }
    }
}
