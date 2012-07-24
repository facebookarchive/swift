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

import com.facebook.swift.service.ThriftClient;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftClientManager.ThriftClientMetadata;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

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

    public <T> void bindThriftClient(Class<T> clientInterface)
    {
        Preconditions.checkNotNull(clientInterface, "clientInterface is null");

        TypeLiteral<ThriftClient<T>> typeLiteral = toThriftClientTypeLiteral(clientInterface);
        ThriftClientProviderProvider<T> provider = new ThriftClientProviderProvider<>(clientInterface, ThriftClientManager.DEFAULT_NAME);
        binder.bind(typeLiteral).toProvider(provider);

        // bind provider to set so later we can export the metadata to JMX
        Multibinder.newSetBinder(binder, ThriftClientProviderProvider.class).addBinding().toInstance(provider);
    }

    public <T> void bindThriftClient(Class<T> clientInterface, Class<? extends Annotation> annotationType)
    {
        Preconditions.checkNotNull(clientInterface, "clientInterface is null");
        TypeLiteral<ThriftClient<T>> typeLiteral = toThriftClientTypeLiteral(clientInterface);

        ThriftClientProviderProvider<T> provider = new ThriftClientProviderProvider<>(clientInterface, annotationType.getSimpleName());
        binder.bind(Key.get(typeLiteral, annotationType)).toProvider(provider);

        // bind provider to set so later we can export the metadata to JMX
        Multibinder.newSetBinder(binder, ThriftClientProviderProvider.class).addBinding().toInstance(provider);
    }

    /**
     * @return  TypeLiteral<ThriftClient<T>>
     */
    private static <T> TypeLiteral<ThriftClient<T>> toThriftClientTypeLiteral(Class<T> clientInterface)
    {
        // build a TypeLiteral<ThriftClientProvider<T>> where T is bound to the actual clientInterface class
        Type javaType = new TypeToken<ThriftClient<T>>(){}
                .where(new TypeParameter<T>(){}, TypeToken.of(clientInterface))
                .getType();
        return (TypeLiteral<ThriftClient<T>>) TypeLiteral.get(javaType);
    }

    public static class ThriftClientProviderProvider<T> implements Provider<ThriftClient<T>>
    {
        private final Class<T> clientType;
        private final String clientName;
        private ThriftClientManager clientManager;

        public ThriftClientProviderProvider(Class<T> clientType, String clientName)
        {
            Preconditions.checkNotNull(clientType, "clientInterface is null");
            Preconditions.checkNotNull(clientName, "clientName is null");
            this.clientType = clientType;
            this.clientName = clientName;
        }

        @Inject
        public void setClientManager(ThriftClientManager clientManager)
        {
            this.clientManager = clientManager;
        }

        @Override
        public ThriftClient<T> get()
        {
            Preconditions.checkState(clientManager != null, "clientManager has not been set");
            return new ThriftClient<>(clientManager, clientType, clientName);
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

            ThriftClientProviderProvider<?> that = (ThriftClientProviderProvider<?>) o;

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
