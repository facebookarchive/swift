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
package com.facebook.swift.service;

import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.nifty.client.NiftyClientConnector;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.airlift.units.Duration;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.channel.Channel;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.annotation.concurrent.Immutable;

import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_CONNECT_TIMEOUT;
import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_READ_TIMEOUT;
import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_WRITE_TIMEOUT;
import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_MAX_FRAME_SIZE;
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
        this.niftyClient = new NiftyClient();
    }

    public <T, C extends NiftyClientChannel> ListenableFuture<T> createClient(
            NiftyClientConnector<C> connector,
            Class<T> type)
    {
        return createClient(connector,
                            type,
                            DEFAULT_CONNECT_TIMEOUT,
                            DEFAULT_READ_TIMEOUT,
                            DEFAULT_WRITE_TIMEOUT,
                            DEFAULT_MAX_FRAME_SIZE,
                            DEFAULT_NAME,
                            null);
    }

    public <T, C extends NiftyClientChannel> ListenableFuture<T> createClient(
            final NiftyClientConnector<C> connector,
            final Class<T> type,
            final Duration connectTimeout,
            final Duration readTimeout,
            final Duration writeTimeout,
            final int maxFrameSize,
            final String clientName,
            HostAndPort socksProxy)
    {
        NiftyClientChannel channel = null;
        try {
            final SettableFuture<T> clientFuture = SettableFuture.create();
            ListenableFuture<C> connectFuture =
                    niftyClient.connectAsync(connector,
                                             connectTimeout,
                                             readTimeout,
                                             writeTimeout,
                                             maxFrameSize,
                                             this.toSocksProxyAddress(socksProxy));
            Futures.addCallback(connectFuture, new FutureCallback<C>()
            {
                @Override
                public void onSuccess(C result)
                {
                    NiftyClientChannel channel = result;

                    if (readTimeout.toMillis() > 0) {
                        channel.setReceiveTimeout(readTimeout);
                    }
                    if (writeTimeout.toMillis() > 0) {
                        channel.setSendTimeout(writeTimeout);
                    }
                    clientFuture.set(createClient(channel, type, Strings.isNullOrEmpty(clientName) ? connector.toString() : clientName));
                }

                @Override
                public void onFailure(Throwable t)
                {
                    clientFuture.setException(t);
                }
            });
            return clientFuture;
        }
        catch (RuntimeException | Error e) {
            if (channel != null) {
                channel.close();
            }
            throw e;
        }
    }

    public <T> T createClient(NiftyClientChannel channel, Class<T> type)
    {
        return createClient(channel, type, DEFAULT_NAME);
    }

    public <T> T createClient(NiftyClientChannel channel, Class<T> type, String name)
    {
        ThriftClientMetadata clientMetadata = clientMetadataCache.getUnchecked(new TypeAndName(type, name));

        String clientDescription = clientMetadata.getName() + " " + channel.toString();

        ThriftInvocationHandler handler = new ThriftInvocationHandler(clientDescription, channel, clientMetadata.getMethodHandlers());

        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{ type, Closeable.class },
                handler
        ));
    }

    private InetSocketAddress toInetSocketAddress(HostAndPort hostAndPort)
    {
        return new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
    }

    private InetSocketAddress toSocksProxyAddress(HostAndPort socksProxy)
    {
        if (socksProxy == null) {
            return null;
        }
        return new InetSocketAddress(socksProxy.getHostText(), socksProxy.getPortOrDefault(SOCKS_DEFAULT_PORT));
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

    /**
     * Returns the {@link NiftyClientChannel} backing a Swift client
     *
     * @throws IllegalArgumentException if the client is not a Swift client
     */
    public NiftyClientChannel getNiftyChannel(Object client)
    {
        try {
            InvocationHandler genericHandler = Proxy.getInvocationHandler(client);
            ThriftInvocationHandler thriftHandler = (ThriftInvocationHandler) genericHandler;
            return thriftHandler.getChannel();
        }
        catch (IllegalArgumentException | ClassCastException e) {
            throw new IllegalArgumentException("Not a swift client object", e);
        }
    }

    /**
     * Returns the remote address that a Swift client is connected to
     *
     * @throws IllegalArgumentException if the client is not a Swift client or is not connected
     * through an internet socket
     */
    public HostAndPort getRemoteAddress(Object client)
    {
        NiftyClientChannel niftyChannel = getNiftyChannel(client);

        try {
            Channel nettyChannel = niftyChannel.getNettyChannel();
            SocketAddress address = nettyChannel.getRemoteAddress();
            InetSocketAddress inetAddress = (InetSocketAddress) address;
            return HostAndPort.fromParts(inetAddress.getHostName(), inetAddress.getPort());
        }
        catch (NullPointerException | ClassCastException e) {
            throw new IllegalArgumentException("Swift client is not connected through an internet socket", e);
        }
    }

    @Immutable
    public static class ThriftClientMetadata
    {
        private final String clientType;
        private final String clientName;
        private final ThriftServiceMetadata thriftServiceMetadata;
        private final Map<Method, ThriftMethodHandler> methodHandlers;

        private ThriftClientMetadata(
                Class<?> clientType,
                String clientName,
                ThriftCodecManager codecManager)
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
        private final TProtocolFactory in;
        private final TProtocolFactory out;

        private final NiftyClientChannel channel;

        private final Map<Method, ThriftMethodHandler> methods;
        private final AtomicInteger sequenceId = new AtomicInteger(1);

        private ThriftInvocationHandler(
                String clientDescription,
                NiftyClientChannel channel,
                Map<Method, ThriftMethodHandler> methods)
        {
            this.clientDescription = clientDescription;
            this.channel = channel;
            this.methods = methods;

            TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
            this.in = protocolFactory;
            this.out = protocolFactory;
        }

        public NiftyClientChannel getChannel()
        {
            return channel;
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
                channel.close();
                return null;
            }

            ThriftMethodHandler methodHandler = methods.get(method);

            try {
                if (methodHandler == null) {
                    throw new TApplicationException(UNKNOWN_METHOD, "Unknown method : '" + method + "'");
                }
                return methodHandler.invoke(in, out, channel, sequenceId.getAndIncrement(), args);
            }
            catch (TException e) {
                Class<? extends TException> thrownType = e.getClass();

                for (Class<?> exceptionType : method.getExceptionTypes()) {
                    if (exceptionType.isAssignableFrom(thrownType)) {
                        throw e;
                    }
                }

                //noinspection InstanceofCatchParameter
                if (e instanceof TApplicationException) {
                    throw new RuntimeTApplicationException(e.getMessage(), (TApplicationException) e);
                }
                //noinspection InstanceofCatchParameter
                if (e instanceof TProtocolException) {
                    throw new RuntimeTProtocolException(e.getMessage(), (TProtocolException) e);
                }
                //noinspection InstanceofCatchParameter
                if (e instanceof TTransportException) {
                    throw new RuntimeTTransportException(e.getMessage(), (TTransportException) e);
                }
                throw new RuntimeTException(e.getMessage(), e);
            }
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
