/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.nifty.server;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

/**
 * SelectorProvider.provider() caches whatever initialized statically, so
 * when running in a test suite simply clearing the property
 * "java.nio.channels.spi.SelectorProvider" would not work to restore
 * other tests to operate on normal selector provider.
 *
 * This is a hack to make it work.
 */
public class DelegateSelectorProvider extends SelectorProvider {
  private static final SelectorProvider original ;
  private static final SelectorProvider deaf = new DeafSelectorProvider();
  private static SelectorProvider delegate ;

  static {
    try {
      // hack to work around compiler complaints about sun.nio.ch.PollSelectorProvider
      // being proprietary
      delegate = original = (SelectorProvider) Class.forName("sun.nio.ch.PollSelectorProvider").newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.setProperty(
      "java.nio.channels.spi.SelectorProvider",
      DelegateSelectorProvider.class.getName()
    );
  }

  public static void init() {
  }

  public static void makeDeaf() {
    delegate = deaf ;
  }

  public static void makeUndeaf() {
    delegate = original;
  }

  @Override
  public DatagramChannel openDatagramChannel() throws IOException {
    return delegate.openDatagramChannel();
  }

  @Override
  public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
    return delegate.openDatagramChannel(family);
  }

  @Override
  public Pipe openPipe() throws IOException {
    return delegate.openPipe();
  }

  @Override
  public AbstractSelector openSelector() throws IOException {
    return delegate.openSelector();
  }

  @Override
  public ServerSocketChannel openServerSocketChannel() throws IOException {
    return delegate.openServerSocketChannel();
  }

  @Override
  public SocketChannel openSocketChannel() throws IOException {
    return delegate.openSocketChannel();
  }

  @Override
  public Channel inheritedChannel() throws IOException {
    return delegate.inheritedChannel();
  }
}
