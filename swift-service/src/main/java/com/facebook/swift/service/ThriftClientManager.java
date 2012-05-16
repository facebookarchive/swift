/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.apache.thrift.TApplicationException.UNKNOWN_METHOD;

public class ThriftClientManager {
  private final ThriftCodecManager codecManager;

  public ThriftClientManager() {
    this(new ThriftCodecManager());
  }

  public ThriftClientManager(ThriftCodecManager codecManager) {
    this.codecManager = codecManager;
  }

  public <T> T createClient(HostAndPort address, Class<T> type) throws TTransportException {
    ThriftServiceMetadata thriftServiceMetadata = new ThriftServiceMetadata(
        type,
        codecManager.getCatalog()
    );
    ImmutableMap.Builder<Method, ThriftMethodHandler> methods = ImmutableMap.builder();
    for (ThriftMethodMetadata methodMetadata : thriftServiceMetadata.getMethods().values()) {
      ThriftMethodHandler methodHandler = new ThriftMethodHandler(
          methodMetadata,
          codecManager
      );
      methods.put(methodMetadata.getMethod(), methodHandler);
    }

    TSocket socket = new TSocket(address.getHostText(), address.getPort());
    socket.open();
    try {
      String clientDescription = thriftServiceMetadata.getName() + " " + address;

      ThriftInvocationHandler handler = new ThriftInvocationHandler(
          clientDescription,
          socket,
          methods.build());

      return (T) Proxy.newProxyInstance(
          type.getClassLoader(),
          new Class<?>[]{type, AutoCloseable.class},
          handler
      );
    } catch(RuntimeException | Error e) {
      socket.close();
      throw e;
    }
  }

  private static class ThriftInvocationHandler implements InvocationHandler {
    private static final Object[] NO_ARGS = new Object[0];
    private final String clientDescription;
    private final TSocket socket;
    private final TProtocol in;
    private final TProtocol out;
    private final Map<Method, ThriftMethodHandler> methods;

    private ThriftInvocationHandler(
        String clientDescription,
        TSocket socket,
        Map<Method, ThriftMethodHandler> methods
    ) {
      this.clientDescription = clientDescription;
      this.socket = socket;
      this.methods = methods;

      TProtocol protocol = new TBinaryProtocol(new TFramedTransport(socket));
      this.in = protocol;
      this.out = protocol;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        socket.close();
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
