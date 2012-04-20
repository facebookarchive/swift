package com.facebook.nifty.server;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: jaxlaw
 * Date: 4/18/12
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class NettyServerTransport {
    private static final Logger log = LoggerFactory.getLogger(NettyServerTransport.class);

    private final int port;
    private final ChannelPipelineFactory pipelineFactory;
    private ServerBootstrap bootstrap;
    private ExecutorService bossExecutor;
    private ExecutorService workerExecutor;
    private Channel serverChannel;
    private final ThriftServerDef def;
    private final NiftyConfig config;

    @Inject
    public NettyServerTransport(final ThriftServerDef def, NiftyConfig config) {
        this.def = def;
        this.config = config;
        this.port = def.getServerPort();
        this.pipelineFactory = new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline cp = Channels.pipeline();
                cp.addLast("debug", new ChannelTracker());
                cp.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(def.getMaxFrameSize(), 0, 4, 0, 4));
                cp.addLast("thriftDecoder", new NettyThriftDecoder());
                cp.addLast("frameEncoder", new LengthFieldPrepender(4));
                cp.addLast("thriftEncoder", new NettyThriftEncoder());
                cp.addLast("dispatcher", new NiftyDispatcher(def));
                return cp;
            }
        };
    }

    public void start() {
        bossExecutor = config.getNumBossThreads() > 0 ? Executors.newFixedThreadPool(config.getNumBossThreads()) : Executors.newCachedThreadPool();
        workerExecutor = config.getNumWorkerThreads() > 0 ? Executors.newFixedThreadPool(config.getNumWorkerThreads()) : Executors.newCachedThreadPool();
        bootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                bossExecutor,
                workerExecutor));
        bootstrap.setPipelineFactory(pipelineFactory);
        log.info("starting server transport at {}", port);
        serverChannel = bootstrap.bind(new InetSocketAddress(port));
    }

    public void stop() {
        log.info("stopping server transport at {}", port);
        // first stop accepting
        serverChannel.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                // stop accepting
                if (bossExecutor != null) {
                    shutdown(NettyServerTransport.this.bossExecutor, "bossExecutor");
                    bossExecutor = null;
                }

                // then stop and process remaining in-flight
                if (def.getExecutor() instanceof ExecutorService) {
                    ExecutorService exe = (ExecutorService) def.getExecutor();
                    shutdown(exe, "dispatcher");
                }

                // finally the reader writer
                if (workerExecutor != null) {
                    shutdown(NettyServerTransport.this.workerExecutor, "workerExecutor");
                    workerExecutor = null;
                }
            }
        });

    }

    private void shutdown(ExecutorService executor, final String name) {
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
