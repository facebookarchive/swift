package com.facebook.nifty.client;

import com.facebook.nifty.client.socks.Socks4ClientBootstrap;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NiftyClient implements Closeable {

  // 1MB default
  private static final int DEFAULT_MAX_FRAME_SIZE = 1048576;

  private final NettyClientConfigBuilder configBuilder;
  private final ExecutorService boss;
  private final ExecutorService worker;
  private final int maxFrameSize;
  private final NioClientSocketChannelFactory channelFactory;
  private InetSocketAddress socksProxyAddress;

  /**
   * Creates a new NiftyClient with defaults : frame size 1MB, 30 secs
   * connect and read timeout and cachedThreadPool for boss and worker.
   */
  public NiftyClient() {
    this(DEFAULT_MAX_FRAME_SIZE);
  }

  public NiftyClient(int maxFrameSize) {
    this(new NettyClientConfigBuilder(),
      MoreExecutors.getExitingExecutorService(makeThreadPool("netty-boss")),
      MoreExecutors.getExitingExecutorService(makeThreadPool("netty-worker")),
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

  public NiftyClient withSocksProxy(InetSocketAddress addr) {
    this.socksProxyAddress = addr;
    return this;
  }

  public ListenableFuture<TNiftyAsyncClientTransport> connectAsync(InetSocketAddress addr) {
    ClientBootstrap bootstrap = createClientBootstrap();
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
  public TNiftyClientTransport connectSync(InetSocketAddress addr) throws TTransportException, InterruptedException {
    return connectSync(addr, 2, 2, TimeUnit.SECONDS);
  }

  public TNiftyClientTransport connectSync(InetSocketAddress addr, long connectTimeout, long readTimeout, TimeUnit unit)
    throws TTransportException, InterruptedException {
    ClientBootstrap bootstrap = createClientBootstrap();
    bootstrap.setOptions(configBuilder.getOptions());
    bootstrap.setPipelineFactory(new NiftyClientChannelPipelineFactory(maxFrameSize));
    ChannelFuture f = bootstrap.connect(addr);
    f.await(
      unit.toMillis(connectTimeout),
      TimeUnit.MILLISECONDS
    );
    Channel channel = f.getChannel();
    if (f.getCause() != null) {
      throw new TTransportException(String.format(
        "unable to connect to %s:%d %s",
        addr.getHostName(), addr.getPort(), socksProxyAddress == null ? "" : "via socks proxy at " + socksProxyAddress
      ), f.getCause());
    }
    if (channel != null) {
      TNiftyClientTransport transport = new TNiftyClientTransport(channel, readTimeout, unit);
      channel.getPipeline().addLast("thrift", transport);
      return transport;
    }
    throw new TTransportException(String.format(
      "unknown error connecting to %s:%d %s",
      addr.getHostName(), addr.getPort(), socksProxyAddress == null ? "" : "via socks proxy at " + socksProxyAddress
    ));
  }

  @Override
  public void close() {
    boss.shutdownNow();
    worker.shutdownNow();
  }

  private ClientBootstrap createClientBootstrap() {
    return this.socksProxyAddress != null ?
      new Socks4ClientBootstrap(channelFactory, this.socksProxyAddress) :
      new ClientBootstrap(channelFactory);
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

  // just the inline version of Executors.newCachedThreadPool()
  // to make MoreExecutors happy.
  private static ThreadPoolExecutor makeThreadPool(String name) {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
      60L, TimeUnit.SECONDS,
      new SynchronousQueue<Runnable>(),
      new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
  }

}
