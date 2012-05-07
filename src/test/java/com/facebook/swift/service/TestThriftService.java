/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.facebook.nifty.guice.NiftyModule;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.scribe.LogEntry;
import com.facebook.swift.service.scribe.ResultCode;
import com.facebook.swift.service.scribe.scribe;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Stage;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Demonstrates creating a Thrift service using Swift.
 */
public class TestThriftService {
  @Test(groups = "fast")
  public void testSwiftService() throws Exception {
    SwiftScribe scribeService = new SwiftScribe();
    TProcessor processor = new ThriftServiceProcessor(scribeService, new ThriftCodecManager());

    ImmutableList<LogEntry> messages = testProcessor(processor);
    assertEquals(scribeService.getMessages(), toSwiftLogEntry(messages));
  }

  @Test(groups = "fast")
  public void testThriftService() throws Exception {
    ThriftScribeService scribeService = new ThriftScribeService();
    TProcessor processor = new scribe.Processor<>(scribeService);

    ImmutableList<LogEntry> messages = testProcessor(processor);
    assertEquals(scribeService.getMessages(), messages);
  }

  private ImmutableList<LogEntry> testProcessor(TProcessor processor) throws TException {
    ImmutableList<LogEntry> messages = ImmutableList.of(
        new LogEntry("hello", "world"),
        new LogEntry("bye", "world")
    );

    int port = getRandomPort();
    NiftyBootstrap bootstrap = createNiftyBootstrap(processor, port);
    try {
      scribe.Client client = createClient(port);
      ResultCode response = client.Log(messages);
      assertEquals(response, ResultCode.OK);
    } finally {
      bootstrap.stop();
    }

    return messages;
  }

  private scribe.Client createClient(int port) throws TTransportException {
    TSocket socket = new TSocket("localhost", port);
    socket.open();
    TBinaryProtocol tp = new TBinaryProtocol(new TFramedTransport(socket));
    return new scribe.Client(tp);
  }

  private NiftyBootstrap createNiftyBootstrap(final TProcessor processor, final int port) {
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

  private int getRandomPort() {
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

  private List<com.facebook.swift.service.LogEntry> toSwiftLogEntry(
      ImmutableList<LogEntry> messages
  ) {
    return Lists.transform(messages, new Function<LogEntry, com.facebook.swift.service.LogEntry>() {
      @Override
      public com.facebook.swift.service.LogEntry apply(@Nullable LogEntry input) {
        return new com.facebook.swift.service.LogEntry(input.category, input.message);
      }
    });
  }

}
