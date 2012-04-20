package com.facebook.nifty.core;

import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builder for the Thrift Server descriptor
 *
 * @author jaxlaw
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
    }

    public ThriftServerDefBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ThriftServerDefBuilder listen(int serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    public ThriftServerDefBuilder speaks(TProtocolFactory tProtocolFactory) {
        this.inProtocolFact = tProtocolFactory;
        this.outProtocolFact = tProtocolFactory;
        return this;
    }

    public ThriftServerDefBuilder withProcessor(TProcessor p) {
        this.processorFactory = new TProcessorFactory(p);
        return this;
    }

    public ThriftServerDefBuilder limitFrameSizeTo(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    public ThriftServerDefBuilder withProcessorFactory(TProcessorFactory processorFactory) {
        this.processorFactory = processorFactory;
        return this;
    }

    public ThriftServerDefBuilder inProtocol(TProtocolFactory tProtocolFactory) {
        this.inProtocolFact = tProtocolFactory;
        return this;
    }

    public ThriftServerDefBuilder outProtocol(TProtocolFactory tProtocolFactory) {
        this.outProtocolFact = tProtocolFactory;
        return this;
    }

    public ThriftServerDefBuilder using(Executor exe) {
        this.executor = exe;
        return this;
    }

    public ThriftServerDef build() {
        if (processorFactory == null) {
            throw new IllegalStateException("processor not defined !");
        }
        return new ThriftServerDef(name, serverPort, maxFrameSize, processorFactory, inProtocolFact, outProtocolFact, executor);
    }
}
