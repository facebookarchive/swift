package com.facebook.nifty.server;

import com.google.inject.Inject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Set;

/**
 * A lifecycle object that manages starting up and shutting down multiple server channels
 *
 * @author jaxlaw
 */
public class NiftyBootstrap {
    private final Set<ThriftServerDef> thriftServerDefs;
    private ArrayList<NettyServerTransport> transports;

    @Inject
    public NiftyBootstrap(Set<ThriftServerDef> thriftServerDefs, NiftyConfig config) {
        this.thriftServerDefs = thriftServerDefs;
        this.transports = new ArrayList<NettyServerTransport>();
        for (ThriftServerDef thriftServerDef : thriftServerDefs) {
            transports.add(new NettyServerTransport(thriftServerDef, config));
        }
    }

    @PostConstruct
    public void start() {
        for (NettyServerTransport transport : transports) {
            transport.start();
        }
    }

    @PreDestroy
    public void stop() {
        for (NettyServerTransport transport : transports) {
            transport.stop();
        }
    }

}
