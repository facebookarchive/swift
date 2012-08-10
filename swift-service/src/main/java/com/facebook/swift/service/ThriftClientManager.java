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
package com.facebook.swift.service;

import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.client.TNiftyClientTransport;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import io.airlift.units.Duration;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import javax.annotation.PreDestroy;
import javax.annotation.concurrent.Immutable;
import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_CONNECT_TIMEOUT;
import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_READ_TIMEOUT;
import static org.apache.thrift.TApplicationException.UNKNOWN_METHOD;

public class ThriftClientManager implements Closeable
{
    public static final String DEFAULT_NAME = "default";
    private static final int SOCKS_DEFAULT_PORT = 1080;

    private final ThriftCodecManager codecManager;
    private final NiftyClient niftyClient;
    private final LoadingCache<TypeAndName, ThriftClientMetadata> clientMetadataCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<TypeAndName, ThriftClientMetadata>()
            {
                @Override
                public ThriftClientMetadata load(TypeAndName typeAndName)
                        throws Exception
                {
                    return new ThriftClientMetadata(typeAndName.getType(), typeAndName.getName(), codecManager);
                }
            });

    public ThriftClientManager()
    {
        this(new ThriftCodecManager());
    }

    public ThriftClientManager(ThriftCodecManager codecManager)
    {
        this.codecManager = codecManager;
        niftyClient = new NiftyClient();
    }

    public <T> T createClient(HostAndPort address, Class<T> type)
            throws TTransportException
    {
        return createClient(address, type, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, DEFAULT_NAME, null);
    }

    public <T> T createClient(HostAndPort address, Class<T> type, Duration connectTimeout, Duration readTimeout, String clientName, HostAndPort socksProxy)
            throws TTransportException
    {
        TNiftyClientTransport transport;
        try {
            transport = niftyClient.connectSync(new InetSocketAddress(address.getHostText(), address.getPort()),
                    (long) connectTimeout.toMillis(),
                    (long) readTimeout.toMillis(),
                    TimeUnit.MILLISECONDS,
                    toSocksProxyAddress(socksProxy));
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Throwables.propagate(e);
        }

        try {
            return createClient(transport, type, address.toString());
        }
        catch (RuntimeException | Error e) {
            transport.close();
            throw e;
        }
    }

    private InetSocketAddress toSocksProxyAddress(HostAndPort socksProxy)
    {
        if (socksProxy == null) {
            return null;
        }
        return new InetSocketAddress(socksProxy.getHostText(), socksProxy.getPortOrDefault(SOCKS_DEFAULT_PORT));
    }

    public <T> T createClient(TTransport transport, Class<T> type, Duration connectTimeout, Duration readTimeout, String clientName)
            throws TTransportException
    {
        return createClient(transport, type, DEFAULT_NAME);
    }

    @SuppressWarnings("unchecked")
    public <T> T createClient(TTransport transport, Class<T> type, String name)
            throws TTransportException
    {
        ThriftClientMetadata clientMetadata = clientMetadataCache.getUnchecked(new TypeAndName(type, name));

        String clientDescription = clientMetadata.getName() + " " + transport.toString();

        ThriftInvocationHandler handler = new ThriftInvocationHandler(
                clientDescription,
                transport,
                clientMetadata.getMethodHandlers());

        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type, Closeable.class},
                handler
        );
    }

    public ThriftClientMetadata getClientMetadata(Class<?> type, String name)
    {
        return clientMetadataCache.getUnchecked(new TypeAndName(type, name));
    }

    @PreDestroy
    public void close()
    {
        niftyClient.close();
    }

    @Immutable
    public static class ThriftClientMetadata
    {
        private final String clientType;
        private final String clientName;
        private final ThriftServiceMetadata thriftServiceMetadata;
        private final Map<Method, ThriftMethodHandler> methodHandlers;

        private ThriftClientMetadata(Class<?> clientType, String clientName, ThriftCodecManager codecManager)
        {
            Preconditions.checkNotNull(clientType, "clientType is null");
            Preconditions.checkNotNull(clientName, "clientName is null");
            Preconditions.checkNotNull(codecManager, "codecManager is null");

            this.clientName = clientName;
            thriftServiceMetadata = new ThriftServiceMetadata(clientType, codecManager.getCatalog());
            this.clientType = thriftServiceMetadata.getName();
            ImmutableMap.Builder<Method, ThriftMethodHandler> methods = ImmutableMap.builder();
            for (ThriftMethodMetadata methodMetadata : thriftServiceMetadata.getMethods().values()) {
                ThriftMethodHandler methodHandler = new ThriftMethodHandler(methodMetadata, codecManager);
                methods.put(methodMetadata.getMethod(), methodHandler);
            }
            methodHandlers = methods.build();
        }

        public String getClientType()
        {
            return clientType;
        }

        public String getClientName()
        {
            return clientName;
        }

        public String getName()
        {
            return thriftServiceMetadata.getName();
        }

        public Map<Method, ThriftMethodHandler> getMethodHandlers()
        {
            return methodHandlers;
        }
    }

    private static class ThriftInvocationHandler implements InvocationHandler
    {
        private static final Object[] NO_ARGS = new Object[0];
        private final String clientDescription;
        private final TTransport transport;
        private final TProtocol in;
        private final TProtocol out;
        private final Map<Method, ThriftMethodHandler> methods;
        private final AtomicInteger sequenceId = new AtomicInteger(1);

        private ThriftInvocationHandler(
                String clientDescription,
                TTransport transport,
                Map<Method, ThriftMethodHandler> methods
        )
        {
            this.clientDescription = clientDescription;
            this.transport = transport;
            this.methods = methods;

            TProtocol protocol = new TBinaryProtocol(transport);
            this.in = protocol;
            this.out = protocol;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable
        {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                    case "toString":
                        return clientDescription;
                    case "equals":
                        return equals(Proxy.getInvocationHandler(args[0]));
                    case "hashCode":
                        return hashCode();
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            if (args == null) {
                args = NO_ARGS;
            }

            if (args.length == 0 && "close".equals(method.getName())) {
                transport.close();
                return null;
            }

            ThriftMethodHandler methodHandler = methods.get(method);
            if (methodHandler == null) {
                throw new TApplicationException(UNKNOWN_METHOD, "Unknown method : '" + method + "'");
            }
            return methodHandler.invoke(in, out, sequenceId.getAndIncrement(), args);
        }
    }

    @Immutable
    private static class TypeAndName
    {
        private final Class<?> type;
        private final String name;

        public TypeAndName(Class<?> type, String name)
        {
            Preconditions.checkNotNull(type, "type is null");
            Preconditions.checkNotNull(name, "name is null");
            this.type = type;
            this.name = name;
        }

        public Class<?> getType()
        {
            return type;
        }

        public String getName()
        {
            return name;
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

            TypeAndName that = (TypeAndName) o;

            if (!name.equals(that.name)) {
                return false;
            }
            if (!type.equals(that.type)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = type.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("TypeAndName");
            sb.append("{type=").append(type);
            sb.append(", name='").append(name).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
