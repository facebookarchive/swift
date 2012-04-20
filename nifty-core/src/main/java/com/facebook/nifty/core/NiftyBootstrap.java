package com.facebook.nifty.core;

import com.facebook.concurrency.NamedThreadFactory;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A lifecycle object that manages starting up and shutting down multiple core channels.
 *
 * @author jaxlaw
 */
public class NiftyBootstrap {
    private static final Logger log = LoggerFactory.getLogger(NiftyBootstrap.class);

    private final Set<ThriftServerDef> thriftServerDefs;
    private ArrayList<NettyServerTransport> transports;
    private ExecutorService bossExecutor;
    private ExecutorService workerExecutor;

    /**
     * This takes a Set of ThriftServerDef. Use Guice Multibinder to inject.
     * @param thriftServerDefs
     */
    @Inject
    public NiftyBootstrap(Set<ThriftServerDef> thriftServerDefs) {
        this.thriftServerDefs = thriftServerDefs;
        this.transports = new ArrayList<NettyServerTransport>();
        for (ThriftServerDef thriftServerDef : thriftServerDefs) {
            transports.add(new NettyServerTransport(thriftServerDef));
        }

    }

    @PostConstruct
    public void start() {
        bossExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("nifty-boss"));
        workerExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("nifty-worker"));
        for (NettyServerTransport transport : transports) {
            transport.start(bossExecutor, workerExecutor);
        }
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        for (NettyServerTransport transport : transports) {
            transport.stop();
        }
        // stop bosses
        if (bossExecutor != null) {
            shutdownExecutor(bossExecutor, "bossExecutor");
            bossExecutor = null;
        }

        // finally the reader writer
        if (workerExecutor != null) {
            shutdownExecutor(workerExecutor, "workerExecutor");
            workerExecutor = null;
        }

    }

    // TODO : make wait time configurable ?
    static void shutdownExecutor(ExecutorService executor, final String name) {
        executor.shutdown();
        try {
            log.info("waiting for " + name + " to shutdown");
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //ignored
            Thread.currentThread().interrupt();
        }
    }

}
