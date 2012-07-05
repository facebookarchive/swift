package com.facebook.nifty.client;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NiftyClient {

  private static final int DEFAULT_MAX_FRAME_SIZE = 1048576;
  private final NettyClientConfigBuilder configBuilder;
  private final ExecutorService boss;
  private final ExecutorService worker;
  private final int maxFrameSize;
  private NioClientSocketChannelFactory channelFactory;

  public NiftyClient() {
    this(DEFAULT_MAX_FRAME_SIZE);
  }

  public NiftyClient(int maxFrameSize) {
    this(new NettyClientConfigBuilder(),
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool(),
      maxFrameSize
    );
  }

  public NiftyClient(
    NettyClientConfigBuilder configBuilder,
    ExecutorService boss,
    ExecutorService worker,
    int maxFrameSize
  ) {
    this.configBuilder = configBuilder;
    this.boss = boss;
    this.worker = worker;
    this.maxFrameSize = maxFrameSize;
    this.channelFactory = new NioClientSocketChannelFactory(boss, worker);
  }

  public ListenableFuture<TNiftyAsyncClientTransport> connectAsync(InetSocketAddress addr) {
    ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
    bootstrap.setOptions(configBuilder.getOptions());
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline cp = Channels.pipeline();
        cp.addLast("frameEncoder", new LengthFieldPrepender(4));
        cp.addLast(
          "frameDecoder", new LengthFieldBasedFrameDecoder(maxFrameSize, 0, 4, 0, 4)
        );
        return cp;
      }
    });
    return new TNiftyFuture(bootstrap.connect(addr));
  }

  // trying to mirror the synchronous nature of TSocket as much as possible here.
  public TNiftyClientTransport connectSync(InetSocketAddress addr)
    throws TTransportException {
    ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
    bootstrap.setOptions(configBuilder.getOptions());
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline cp = Channels.pipeline();
        cp.addLast("frameEncoder", new LengthFieldPrepender(4));
        cp.addLast(
          "frameDecoder", new LengthFieldBasedFrameDecoder(maxFrameSize, 0, 4, 0, 4)
        );
        return cp;
      }
    });
    ChannelFuture f = bootstrap.connect(addr);
    final CountDownLatch latch = new CountDownLatch(1);
    final Channel channel[] = new Channel[1];
    final Throwable throwable[] = new Throwable[1];
    f.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
          channel[0] = future.getChannel();
        } else {
          throwable[0] = future.getCause();
        }
        latch.countDown();
      }
    });
    try {
      latch.await(
        f.getChannel().getConfig().getConnectTimeoutMillis(),
        TimeUnit.MILLISECONDS
      );
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    if (throwable[0] != null) {
      throw new TTransportException(String.format("unable to connect to %s:%d", addr.getHostName(), addr.getPort()), throwable[0]);
    }
    if (channel[0] != null) {
      TNiftyClientTransport transport = new TNiftyClientTransport(channel[0]);
      channel[0].getPipeline().addLast("thrift", transport);
      return transport;
    }
    throw new TTransportException(String.format("unknown error connecting to %s:%d", addr.getHostName(), addr.getPort()));
  }

  private static class TNiftyFuture
    extends AbstractFuture<TNiftyAsyncClientTransport> {
    private TNiftyFuture(ChannelFuture channelFuture) {
      channelFuture.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (future.isSuccess()) {
            set(new TNiftyAsyncClientTransport(future.getChannel()));
          } else if (future.isCancelled()) {
            cancel(true);
          } else {
            setException(future.getCause());
          }
        }
      });
    }
  }

  public void shutdown() {
    boss.shutdownNow();
    worker.shutdownNow();
  }



}
