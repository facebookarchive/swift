package com.facebook.nifty.core;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
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
 */
public class NettyServerTransport {
  private static final Logger log = LoggerFactory.getLogger(NettyServerTransport.class);

  private final int port;
  private final ChannelPipelineFactory pipelineFactory;
  private ServerBootstrap bootstrap;
  private Channel serverChannel;
  private final ThriftServerDef def;
  private final NettyConfigBuilder configBuilder;

  @Inject
  public NettyServerTransport(final ThriftServerDef def, NettyConfigBuilder configBuilder, final ChannelGroup allChannels) {
    this.def = def;
    this.configBuilder = configBuilder;
    this.port = def.getServerPort();
    if (def.isHeaderTransport()) {
      throw new UnsupportedOperationException("ASF version does not support THeaderTransport !");
    } else {
      this.pipelineFactory = new ChannelPipelineFactory() {
        @Override
        public ChannelPipeline getPipeline() throws Exception {
          ChannelPipeline cp = Channels.pipeline();
          cp.addLast(ChannelStatistics.NAME, new ChannelStatistics(allChannels));
          cp.addLast(
            "frameDecoder", new LengthFieldBasedFrameDecoder(def.getMaxFrameSize(), 0, 4, 0, 4)
          );
          cp.addLast("thriftDecoder", new NettyThriftDecoder());
          cp.addLast("frameEncoder", new LengthFieldPrepender(4));
          cp.addLast("dispatcher", new NiftyDispatcher(def));
          return cp;
        }
      };
    }

  }

  public void start(ExecutorService bossExecutor, ExecutorService workerExecutor) {
    bootstrap = new ServerBootstrap(
      new NioServerSocketChannelFactory(
        bossExecutor,
        workerExecutor
      )
    );
    bootstrap.setOptions(configBuilder.getOptions());
    bootstrap.setPipelineFactory(pipelineFactory);
    log.info("starting transport {}:{}", def.getName(), port);
    serverChannel = bootstrap.bind(new InetSocketAddress(port));
  }

  public void stop() throws InterruptedException {
    if (serverChannel != null) {
      log.info("stopping transport {}:{}", def.getName(), port);
      // first stop accepting
      final CountDownLatch latch = new CountDownLatch(1);
      serverChannel.close().addListener(
        new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            // stop and process remaining in-flight invocations
            if (def.getExecutor() instanceof ExecutorService) {
              ExecutorService exe = (ExecutorService) def.getExecutor();
              NiftyBootstrap.shutdownExecutor(exe, "dispatcher");
            }
            latch.countDown();
          }
        }
      );
      latch.await();
      serverChannel = null;
    }
  }


}
