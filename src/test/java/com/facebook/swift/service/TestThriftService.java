/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import com.facebook.logging.Logger;
import com.facebook.logging.LoggerImpl;
import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.facebook.nifty.guice.NiftyModule;
import com.facebook.swift.ThriftCodecManager;
import com.facebook.swift.ThriftConstructor;
import com.facebook.swift.ThriftField;
import com.facebook.swift.ThriftStruct;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import com.facebook.swift.service.scribe.LogEntry;
import com.facebook.swift.service.scribe.ResultCode;
import com.facebook.swift.service.scribe.scribe;
import com.google.inject.Guice;
import com.google.inject.Stage;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

public class TestThriftService {
  private static final Logger log = LoggerImpl.getLogger(TestThriftService.class);

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

  @Test(groups = "fast")
  public void testSwiftService() throws Exception {
    ThriftCodecManager codecManager = new ThriftCodecManager();
    TProcessor processor = new ThriftServiceProcessor(
        new ScribeService(),
        new ThriftServiceMetadata(ScribeService.class, codecManager.getCatalog()),
        codecManager
    );

    testProcessor(processor);
  }

  @Test(groups = "fast")
  public void testThriftService() throws Exception {
    TProcessor processor = new scribe.Processor<>(new TestScribeService());
    testProcessor(processor);
  }

  private void testProcessor(TProcessor processor) throws TException {
    int port = getRandomPort();
    NiftyBootstrap bootstrap = createNiftyBootstrap(processor, port);
    try {
      scribe.Client client = makeClient(port);
      client.Log(Arrays.asList(new LogEntry("hello", "world")));
    } finally {
      bootstrap.stop();
    }
  }

  private scribe.Client makeClient(int port) throws TTransportException {
    TSocket socket = new TSocket("localhost", port);
    socket.open();
    TBinaryProtocol tp = new TBinaryProtocol(new TFramedTransport(socket));
    return new scribe.Client(tp);
  }

  @ThriftService("scribe")
  public static class ScribeService {
    @ThriftMethod("Log")
    public ResultCode log(List<LogEntryStruct> messages) {
      for (LogEntryStruct message : messages) {
        log.info("%s: %s", message.getCategory(), message.getMessage());
      }
      return ResultCode.OK;
    }
  }

  private static class TestScribeService implements scribe.Iface {
    @Override
    public ResultCode Log(List<LogEntry> messages)
        throws TException {
      for (LogEntry message : messages) {
        log.info("%s: %s", message.getCategory(), message.getMessage());
      }
      return ResultCode.OK;
    }
  }

  @ThriftStruct
  public static class LogEntryStruct {
    private final String category;
    private final String message;

    @ThriftConstructor
    public LogEntryStruct(
        @ThriftField(name = "category") String category,
        @ThriftField(name = "message") String message) {
      this.category = category;
      this.message = message;
    }

    @ThriftField(id = 1)
    public String getCategory() {
      return category;
    }

    @ThriftField(id = 2)
    public String getMessage() {
      return message;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("LogEntryStruct");
      sb.append("{category='").append(category).append('\'');
      sb.append(", message='").append(message).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }
}
