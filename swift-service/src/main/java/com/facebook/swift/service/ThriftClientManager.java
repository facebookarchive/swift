/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.client.TNiftyClientTransport;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Map;

import static org.apache.thrift.TApplicationException.UNKNOWN_METHOD;

public class ThriftClientManager implements AutoCloseable
{
    private final ThriftCodecManager codecManager;
    private NiftyClient niftyClient;

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
        // build method index
        ThriftServiceMetadata thriftServiceMetadata = new ThriftServiceMetadata(type, codecManager.getCatalog()
        );
        ImmutableMap.Builder<Method, ThriftMethodHandler> methods = ImmutableMap.builder();
        for (ThriftMethodMetadata methodMetadata : thriftServiceMetadata.getMethods().values()) {
            ThriftMethodHandler methodHandler = new ThriftMethodHandler(methodMetadata, codecManager);
            methods.put(methodMetadata.getMethod(), methodHandler);
        }

        TNiftyClientTransport transport = niftyClient.connectSync(new InetSocketAddress(address.getHostText(), address.getPort()));
        try {
            String clientDescription = thriftServiceMetadata.getName() + " " + address;

            ThriftInvocationHandler handler = new ThriftInvocationHandler(
                    clientDescription,
                    transport,
                    methods.build());

            return (T) Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[]{type, AutoCloseable.class},
                    handler
            );
        }
        catch (RuntimeException | Error e) {
            transport.close();
            throw e;
        }
    }

    @PreDestroy
    public void close()
    {
        niftyClient.shutdown();
    }

    private static class ThriftInvocationHandler implements InvocationHandler
    {
        private static final Object[] NO_ARGS = new Object[0];
        private final String clientDescription;
        private final TTransport transport;
        private final TProtocol in;
        private final TProtocol out;
        private final Map<Method, ThriftMethodHandler> methods;

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
            return methodHandler.invoke(in, out, args);
        }
    }
}
