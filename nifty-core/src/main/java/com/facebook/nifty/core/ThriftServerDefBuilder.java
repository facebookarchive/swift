package com.facebook.nifty.core;

import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builder for the Thrift Server descriptor. Example :
 * <code>
 * new ThriftServerDefBuilder()
 * .listen(config.getServerPort())
 * .limitFrameSizeTo(config.getMaxFrameSize())
 * .withProcessor(new FacebookService.Processor(new MyFacebookBase()))
 * .using(Executors.newFixedThreadPool(5))
 * .build();
 * <p/>
 * <p/>
 * You can then pass ThriftServerDef to guice via a multibinder.
 * <p/>
 * </code>
 *
 */
public class ThriftServerDefBuilder {
  private static final AtomicInteger ID = new AtomicInteger(1);
  private int serverPort;
  private int maxFrameSize;
  private TProcessorFactory processorFactory;
  private TProtocolFactory inProtocolFact;
  private TProtocolFactory outProtocolFact;
  private Executor executor;
  private String name = "nifty-" + ID.getAndIncrement();
  private boolean useHeaderTransport;

  /**
   * Create a ThriftServerDefBuilder with common defaults
   */
  public ThriftServerDefBuilder() {
    this.serverPort = 8080;
    this.maxFrameSize = 1048576;
    this.inProtocolFact = new TBinaryProtocol.Factory();
    this.outProtocolFact = new TBinaryProtocol.Factory();
    this.executor = new Executor() {
      @Override
      public void execute(Runnable runnable) {
        runnable.run();
      }
    };
    this.useHeaderTransport = false;
  }

  /**
   * Give the endpoint a more meaningful name.
   *
   * @param name
   * @return
   */
  public ThriftServerDefBuilder name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Listen to this port.
   *
   * @param serverPort
   * @return
   */
  public ThriftServerDefBuilder listen(int serverPort) {
    this.serverPort = serverPort;
    return this;
  }

  /**
   * Specify protocolFactory for both input and output
   *
   * @param tProtocolFactory
   * @return
   */
  public ThriftServerDefBuilder speaks(TProtocolFactory tProtocolFactory) {
    this.inProtocolFact = tProtocolFactory;
    this.outProtocolFact = tProtocolFactory;
    return this;
  }

  /**
   * Specify the TProcessor.
   *
   * @param p
   * @return
   */
  public ThriftServerDefBuilder withProcessor(TProcessor p) {
    this.processorFactory = new TProcessorFactory(p);
    return this;
  }

  /**
   * Set frame size limit.  Default is 1M
   *
   * @param maxFrameSize
   * @return
   */
  public ThriftServerDefBuilder limitFrameSizeTo(int maxFrameSize) {
    this.maxFrameSize = maxFrameSize;
    return this;
  }

  /**
   * Anohter way to specify the TProcessor.
   *
   * @param processorFactory
   * @return
   */
  public ThriftServerDefBuilder withProcessorFactory(TProcessorFactory processorFactory) {
    this.processorFactory = processorFactory;
    return this;
  }

  /**
   * Specify only the input protocol.
   *
   * @param tProtocolFactory
   * @return
   */
  public ThriftServerDefBuilder inProtocol(TProtocolFactory tProtocolFactory) {
    this.inProtocolFact = tProtocolFactory;
    return this;
  }

  /**
   * Specify only the output protocol.
   *
   * @param tProtocolFactory
   * @return
   */
  public ThriftServerDefBuilder outProtocol(TProtocolFactory tProtocolFactory) {
    this.outProtocolFact = tProtocolFactory;
    return this;
  }

  /**
   * Specify an executor for thrift processor invocations ( i.e. = THaHsServer )
   * By default invocation happens in Netty single thread
   * ( i.e. = TNonBlockingServer )
   *
   * @param exe
   * @return
   */
  public ThriftServerDefBuilder using(Executor exe) {
    this.executor = exe;
    return this;
  }

  public ThriftServerDefBuilder usingHeaderTransport() {
    this.useHeaderTransport = true;
    return this;
  }

  /**
   * Build the ThriftServerDef
   *
   * @return
   */
  public ThriftServerDef build() {
    if (processorFactory == null) {
      throw new IllegalStateException("processor not defined !");
    }
    return new ThriftServerDef(
      name,
      serverPort,
      maxFrameSize,
      processorFactory,
      inProtocolFact,
      outProtocolFact,
      useHeaderTransport,
      executor
    );
  }
}
