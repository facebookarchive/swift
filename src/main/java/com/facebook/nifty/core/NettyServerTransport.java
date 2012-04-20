package com.facebook.nifty.core;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * A core channel the decode framed Thrift message, dispatches to the TProcessor given
 * and then encode message back to Thrift frame.
 *
 * @author jaxlaw
 */
public class NettyServerTransport {
    private static final Logger log = LoggerFactory.getLogger(NettyServerTransport.class);

    private final int port;
    private final ChannelPipelineFactory pipelineFactory;
    private ServerBootstrap bootstrap;
    private Channel serverChannel;
    private final ThriftServerDef def;

    @Inject
    public NettyServerTransport(final ThriftServerDef def) {
        this.def = def;
        this.port = def.getServerPort();
        this.pipelineFactory = new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline cp = Channels.pipeline();
                cp.addLast(ChannelStatistics.NAME, new ChannelStatistics());
                cp.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(def.getMaxFrameSize(), 0, 4, 0, 4));
                cp.addLast("thriftDecoder", new NettyThriftDecoder());
                cp.addLast("frameEncoder", new LengthFieldPrepender(4));
                cp.addLast("thriftEncoder", new NettyThriftEncoder());
                cp.addLast("dispatcher", new NiftyDispatcher(def));
                return cp;
            }
        };
    }

    public void start(ExecutorService bossExecutor, ExecutorService workerExecutor) {
        bootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                bossExecutor,
                workerExecutor));
        bootstrap.setPipelineFactory(pipelineFactory);
        log.info("starting core transport at {}", port);
        serverChannel = bootstrap.bind(new InetSocketAddress(port));
    }

    public void stop() throws InterruptedException {
        log.info("stopping core transport at {}", port);
        // first stop accepting
        final CountDownLatch latch = new CountDownLatch(1);
        serverChannel.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                // stop and process remaining in-flight invocations
                if (def.getExecutor() instanceof ExecutorService) {
                    ExecutorService exe = (ExecutorService) def.getExecutor();
                    NiftyBootstrap.shutdownExecutor(exe, "dispatcher");
                }
                latch.countDown();
            }
        });
        latch.await();
    }


}
