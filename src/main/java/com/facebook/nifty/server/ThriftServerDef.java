package com.facebook.nifty.server;

import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TProtocolFactory;

import java.util.concurrent.Executor;

/**
 * Descriptor for a Thrift Server
 *
 * @author jaxlaw
 */
public class ThriftServerDef {
    private final int serverPort;
    private final int maxFrameSize;
    private final TProcessorFactory processorFactory;
    private final TProtocolFactory inProtoalFact;
    private final TProtocolFactory outProtoalFact;
    private final Executor executor;
    private final String name;

    public ThriftServerDef(String name, int serverPort, int maxFrameSize, TProcessorFactory factory, TProtocolFactory inProtoalFact, TProtocolFactory outProtoalFact, Executor executor) {
        this.name = name;
        this.serverPort = serverPort;
        this.maxFrameSize = maxFrameSize;
        this.processorFactory = factory;
        this.inProtoalFact = inProtoalFact;
        this.outProtoalFact = outProtoalFact;
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
        return inProtoalFact;
    }

    public TProtocolFactory getOutProtocolFactory() {
        return outProtoalFact;
    }

    public Executor getExecutor() {
        return executor;
    }

    public String getName() {
        return name;
    }
}
