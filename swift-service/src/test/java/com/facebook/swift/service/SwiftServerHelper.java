/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.facebook.nifty.guice.NiftyModule;
import com.google.inject.Guice;
import com.google.inject.Stage;
import org.apache.thrift.TProcessor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public final class SwiftServerHelper {
  private SwiftServerHelper() {
  }

  public static NiftyBootstrap createNiftyBootstrap(final TProcessor processor, final int port) {
    NiftyBootstrap bootstrap = Guice.createInjector(
        Stage.PRODUCTION,
        new NiftyModule() {
          @Override
          protected void configureNifty() {
            bind().toInstance(
                new ThriftServerDefBuilder()
                    .listen(port)
                    .withProcessor(processor)
                    .build()
            );
          }
        }
    ).getInstance(NiftyBootstrap.class);

    bootstrap.start();
    return bootstrap;
  }

  public static int getRandomPort() {
    try {
      ServerSocket s = new ServerSocket();
      s.bind(new InetSocketAddress(0));
      int port = s.getLocalPort();
      s.close();
      return port;
    } catch (IOException e) {
      return 8080;
    }
  }
}
