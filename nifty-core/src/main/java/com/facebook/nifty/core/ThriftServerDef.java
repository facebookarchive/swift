package com.facebook.nifty.core;

import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TProtocolFactory;

import java.util.concurrent.Executor;

/**
 * Descriptor for a Thrift Server. This defines a listener port that Nifty need to start a Thrift endpoint.
 *
 */
public class ThriftServerDef {
  private final int serverPort;
  private final int maxFrameSize;
  private final TProcessorFactory processorFactory;
  private final TProtocolFactory inProtocolFact;
  private final TProtocolFactory outProtocolFact;
  private final boolean headerTransport;
  private final Executor executor;
  private final String name;

  public ThriftServerDef(
    String name,
    int serverPort,
    int maxFrameSize,
    TProcessorFactory factory,
    TProtocolFactory inProtoalFact,
    TProtocolFactory outProtoalFact,
    boolean useHeaderTransport,
    Executor executor
  ) {
    this.name = name;
    this.serverPort = serverPort;
    this.maxFrameSize = maxFrameSize;
    this.processorFactory = factory;
    this.inProtocolFact = inProtoalFact;
    this.outProtocolFact = outProtoalFact;
    this.headerTransport = useHeaderTransport;
    this.executor = executor;
  }

  public int getServerPort() {
    return serverPort;
  }

  public int getMaxFrameSize() {
    return maxFrameSize;
  }

  public TProcessorFactory getProcessorFactory() {
    return processorFactory;
  }

  public TProtocolFactory getInProtocolFactory() {
    return inProtocolFact;
  }

  public TProtocolFactory getOutProtocolFactory() {
    return outProtocolFact;
  }

  public boolean isHeaderTransport() {
    return headerTransport;
  }

  public Executor getExecutor() {
    return executor;
  }

  public String getName() {
    return name;
  }
}
