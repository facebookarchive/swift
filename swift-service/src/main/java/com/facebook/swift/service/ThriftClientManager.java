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

import com.facebook.nifty.client.ClientRequestContext;
import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.nifty.client.NiftyClientConnector;
import com.facebook.nifty.client.NiftyClientRequestContext;
import com.facebook.nifty.client.RequestChannel;
import com.facebook.nifty.core.TChannelBufferInputTransport;
import com.facebook.nifty.core.TChannelBufferOutputTransport;
import com.facebook.nifty.duplex.TProtocolPair;
import com.facebook.nifty.duplex.TTransportPair;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import io.airlift.units.Duration;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.channel.Channel;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotNull;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.facebook.nifty.duplex.TTransportPair.fromSeparateTransports;
import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_CONNECT_TIMEOUT;
import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_MAX_FRAME_SIZE;
import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_READ_TIMEOUT;
import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_RECEIVE_TIMEOUT;
import static com.facebook.swift.service.ThriftClientConfig.DEFAULT_WRITE_TIMEOUT;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.thrift.TApplicationException.UNKNOWN_METHOD;

@ThreadSafe
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

    private final Set<ThriftClientEventHandler> globalEventHandlers;

    public ThriftClientManager()
    {
        this(new ThriftCodecManager());
    }

    public ThriftClientManager(ClassLoader parent)
    {
        this(new ThriftCodecManager(parent));
    }

    public ThriftClientManager(ThriftCodecManager codecManager)
    {
        this(codecManager, new NiftyClient(), ImmutableSet.<ThriftClientEventHandler>of());
    }

    @Inject
    public ThriftClientManager(ThriftCodecManager codecManager, NiftyClient niftyClient, Set<ThriftClientEventHandler> globalEventHandlers)
    {
        this.codecManager = checkNotNull(codecManager, "codecManager is null");
        this.niftyClient = checkNotNull(niftyClient, "niftyClient is null");
        this.globalEventHandlers = checkNotNull(globalEventHandlers, "globalEventHandlers is null");
    }

    public <C extends NiftyClientChannel> ListenableFuture<C> createChannel(
            NiftyClientConnector<C> connector)
    {
        return createChannel(connector,
                             DEFAULT_CONNECT_TIMEOUT,
                             DEFAULT_RECEIVE_TIMEOUT,
                             DEFAULT_READ_TIMEOUT,
                             DEFAULT_WRITE_TIMEOUT,
                             DEFAULT_MAX_FRAME_SIZE,
                             getDefaultSocksProxy());
    }

    public <C extends NiftyClientChannel> ListenableFuture<C> createChannel(
            final NiftyClientConnector<C> connector,
            @Nullable final Duration connectTimeout,
            @Nullable final Duration receiveTimeout,
            @Nullable final Duration readTimeout,
            @Nullable final Duration writeTimeout,
            final int maxFrameSize,
            @Nullable HostAndPort socksProxy)
    {
        final ListenableFuture<C> connectFuture = niftyClient.connectAsync(
                connector,
                connectTimeout,
                receiveTimeout,
                readTimeout,
                writeTimeout,
                maxFrameSize,
                socksProxy);

        return connectFuture;
    }

    public <T, C extends NiftyClientChannel> ListenableFuture<T> createClient(
            NiftyClientConnector<C> connector,
            Class<T> type)
    {
        return createClient(
                connector,
                type,
                DEFAULT_CONNECT_TIMEOUT,
                DEFAULT_RECEIVE_TIMEOUT,
                DEFAULT_READ_TIMEOUT,
                DEFAULT_WRITE_TIMEOUT,
                DEFAULT_MAX_FRAME_SIZE,
                DEFAULT_NAME,
                ImmutableList.<ThriftClientEventHandler>of(),
                getDefaultSocksProxy());
    }

    /**
     * @deprecated Use {@link ThriftClientManager#createClient(NiftyClientConnector, Class, Duration, Duration, Duration, Duration, int, String, List, HostAndPort)}.
     */
    @Deprecated
    public <T, C extends NiftyClientChannel> ListenableFuture<T> createClient(
            final NiftyClientConnector<C> connector,
            final Class<T> type,
            @Nullable final Duration connectTimeout,
            @Nullable final Duration readTimeout,
            @Nullable final Duration writeTimeout,
            final int maxFrameSize,
            @Nullable final String clientName,
            final List<? extends ThriftClientEventHandler> eventHandlers,
            @Nullable HostAndPort socksProxy)
    {
        return createClient(
            connector,
            type,
            connectTimeout,
            readTimeout,
            readTimeout,
            writeTimeout,
            maxFrameSize,
            clientName,
            eventHandlers,
            socksProxy);
    }

    public <T, C extends NiftyClientChannel> ListenableFuture<T> createClient(
            final NiftyClientConnector<C> connector,
            final Class<T> type,
            @Nullable final Duration connectTimeout,
            @Nullable final Duration receiveTimeout,
            @Nullable final Duration readTimeout,
            @Nullable final Duration writeTimeout,
            final int maxFrameSize,
            @Nullable final String clientName,
            final List<? extends ThriftClientEventHandler> eventHandlers,
            @Nullable HostAndPort socksProxy)
    {
        checkNotNull(connector, "connector is null");
        checkNotNull(type, "type is null");
        checkNotNull(eventHandlers, "eventHandlers is null");

        final ListenableFuture<C> connectFuture = createChannel(
                connector,
                connectTimeout,
                receiveTimeout,
                readTimeout,
                writeTimeout,
                maxFrameSize,
                socksProxy);

        ListenableFuture<T> clientFuture = Futures.transform(connectFuture, new Function<C, T>() {
            @Nullable
            @Override
            public T apply(@NotNull C channel)
            {
                String name = Strings.isNullOrEmpty(clientName) ? DEFAULT_NAME : clientName;

                try {
                    return createClient(channel, type, name, eventHandlers);
                }
                catch (Throwable t) {
                    // The channel was created successfully, but client creation failed so the
                    // channel must be closed now
                    channel.close();
                    throw t;
                }
            }
        });

        return clientFuture;
    }

    public <T> T createClient(NiftyClientChannel channel, Class<T> type)
    {
        return createClient(channel, type, DEFAULT_NAME, ImmutableList.<ThriftClientEventHandler>of());
    }

    public <T> T createClient(NiftyClientChannel channel, Class<T> type, List<? extends ThriftClientEventHandler> eventHandlers)
    {
        return createClient(channel, type, DEFAULT_NAME, eventHandlers);
    }

    public <T> T createClient(RequestChannel channel, Class<T> type, String name, List<? extends ThriftClientEventHandler> eventHandlers)
    {
        checkNotNull(channel, "channel is null");
        checkNotNull(type, "type is null");
        checkNotNull(name, "name is null");
        checkNotNull(eventHandlers, "eventHandlers is null");

        ThriftClientMetadata clientMetadata = clientMetadataCache.getUnchecked(new TypeAndName(type, name));

        String clientDescription = clientMetadata.getName() + " " + channel.toString();

        ThriftInvocationHandler handler = new ThriftInvocationHandler(clientDescription, channel,
                clientMetadata.getMethodHandlers(),
                ImmutableList.<ThriftClientEventHandler>builder().addAll(globalEventHandlers).addAll(eventHandlers).build());

        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{ type, Closeable.class },
                handler
        ));
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

    public HostAndPort getDefaultSocksProxy()
    {
        return niftyClient.getDefaultSocksProxyAddress();
    }

    /**
     * Returns the {@link RequestChannel} backing a Swift client
     *
     * @throws IllegalArgumentException if the client is not a Swift client
     */
    public RequestChannel getRequestChannel(Object client)
    {
        try {
            InvocationHandler genericHandler = Proxy.getInvocationHandler(client);
            ThriftInvocationHandler thriftHandler = (ThriftInvocationHandler) genericHandler;
            return thriftHandler.getChannel();
        }
        catch (IllegalArgumentException | ClassCastException e) {
            throw new IllegalArgumentException("Invalid swift client object", e);
        }
    }

    /**
     * Returns the {@link NiftyClientChannel} backing a Swift client
     *
     * @throws IllegalArgumentException if the client is not using a {@link com.facebook.nifty.client.NiftyClientChannel}
     *
     * @deprecated Use {@link #getRequestChannel} instead, and cast the result to a {@link NiftyClientChannel} if necessary
     */
    public NiftyClientChannel getNiftyChannel(Object client)
    {
        try {
            return NiftyClientChannel.class.cast(getRequestChannel(client));
        }
        catch (ClassCastException e) {
            throw new IllegalArgumentException("The swift client uses a channel that is not a NiftyClientChannel", e);
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
            return HostAndPort.fromParts(inetAddress.getHostString(), inetAddress.getPort());
        }
        catch (NullPointerException | ClassCastException e) {
            throw new IllegalArgumentException("Invalid swift client object", e);
        }
    }

    public TProtocol getOutputProtocol(Object client)
    {
        try {
            InvocationHandler genericHandler = Proxy.getInvocationHandler(client);
            ThriftInvocationHandler thriftHandler = (ThriftInvocationHandler) genericHandler;
            return thriftHandler.getOutputProtocol();
        }
        catch (IllegalArgumentException | ClassCastException e) {
            throw new IllegalArgumentException("Invalid swift client object", e);
        }
    }

    public TProtocol getInputProtocol(Object client)
    {
        try {
            InvocationHandler genericHandler = Proxy.getInvocationHandler(client);
            ThriftInvocationHandler thriftHandler = (ThriftInvocationHandler) genericHandler;
            return thriftHandler.getInputProtocol();
        }
        catch (IllegalArgumentException | ClassCastException e) {
            throw new IllegalArgumentException("Invalid swift client object", e);
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

        private final RequestChannel channel;

        private final Map<Method, ThriftMethodHandler> methods;
        private final AtomicInteger sequenceId = new AtomicInteger(1);
        private final List<? extends ThriftClientEventHandler> eventHandlers;
        private final TChannelBufferInputTransport inputTransport;
        private final TChannelBufferOutputTransport outputTransport;
        private final TProtocol inputProtocol;
        private final TProtocol outputProtocol;

        private ThriftInvocationHandler(
                String clientDescription,
                RequestChannel channel,
                Map<Method, ThriftMethodHandler> methods,
                List<? extends ThriftClientEventHandler> eventHandlers)
        {
            this.clientDescription = clientDescription;
            this.channel = channel;
            this.methods = methods;
            this.eventHandlers = eventHandlers;

            this.inputTransport = new TChannelBufferInputTransport();
            this.outputTransport = new TChannelBufferOutputTransport();

            TTransportPair transportPair = fromSeparateTransports(this.inputTransport, this.outputTransport);
            TProtocolPair protocolPair = channel.getProtocolFactory().getProtocolPair(transportPair);
            this.inputProtocol = protocolPair.getInputProtocol();
            this.outputProtocol = protocolPair.getOutputProtocol();
        }

        public RequestChannel getChannel()
        {
            return channel;
        }

        public TProtocol getOutputProtocol()
        {
            return outputProtocol;
        }

        public TProtocol getInputProtocol()
        {
            return inputProtocol;
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

                if (channel.hasError()) {
                    throw new TTransportException(channel.getError());
                }

                SocketAddress remoteAddress = null;
                // Can only get remote address if this is a nifty channel, plain RequestChannel does
                // not support it
                if (channel instanceof NiftyClientChannel) {
                    NiftyClientChannel niftyClientChannel = (NiftyClientChannel) channel;
                    remoteAddress = niftyClientChannel.getNettyChannel().getRemoteAddress();
                }

                ClientRequestContext requestContext = new NiftyClientRequestContext(getInputProtocol(), getOutputProtocol(), channel, remoteAddress);
                ClientContextChain context = new ClientContextChain(eventHandlers, methodHandler.getQualifiedName(), requestContext);
                return methodHandler.invoke(channel,
                                            inputTransport,
                                            outputTransport,
                                            inputProtocol,
                                            outputProtocol,
                                            sequenceId.getAndIncrement(),
                                            context,
                                            args);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeTException("Thread interrupted", new TException(e));
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
