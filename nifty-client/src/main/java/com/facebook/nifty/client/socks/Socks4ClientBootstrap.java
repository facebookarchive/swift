package com.facebook.nifty.client.socks;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.FixedLengthFrameDecoder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static com.facebook.nifty.client.socks.SocksProtocols.createSock4aPacket;
import static com.facebook.nifty.client.socks.SocksProtocols.createSocks4packet;

/**
 * ClientBootstrap for connecting via SOCKS proxy.
 * Currently only SOCK4 is supported since we don't do authentication anyway.
 *
 * See http://en.wikipedia.org/wiki/SOCKS
 *
 */
public class Socks4ClientBootstrap extends ClientBootstrap {
  static final String FRAME_DECODER = "frameDecoder";
  static final String HANDSHAKE = "handshake";

  private final InetSocketAddress socksProxyAddr;

  public Socks4ClientBootstrap(ChannelFactory channelFactory, InetSocketAddress socksProxyAddr) {
    super(channelFactory);
    this.socksProxyAddr = socksProxyAddr;
  }

  public Socks4ClientBootstrap(InetSocketAddress socksProxyAddr) {
    this.socksProxyAddr = socksProxyAddr;
    super.setPipeline(getPipeline());
  }

  /**
   * Hijack super class's pipelineFactory and return our own that
   * does the connect to SOCKS proxy and does the handshake.
   *
   * @return
   */
  @Override
  public ChannelPipelineFactory getPipelineFactory() {
    return new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline() throws Exception {
        final ChannelPipeline cp = Channels.pipeline();
        cp.addLast(FRAME_DECODER, new FixedLengthFrameDecoder(8));
        cp.addLast(HANDSHAKE, new Socks4HandshakeHandler(Socks4ClientBootstrap.super.getPipelineFactory()));
        return cp;
      }
    };
  }

  /**
   * Hijack the connect method to connect to socks proxy and then
   * send the connection handshake once connection to proxy is established.
   *
   * @param remoteAddress
   * @return returns a ChannelFuture, it will be ready once the connection to
   * socks and the remote address is established ( i.e. after the handshake completes )
   */
  @Override
  public ChannelFuture connect(final SocketAddress remoteAddress) {
    if (!(remoteAddress instanceof InetSocketAddress)) {
      throw new IllegalArgumentException("expecting InetSocketAddress");
    }
    final SettableChannelFuture settableChannelFuture = new SettableChannelFuture();
    super.connect(socksProxyAddr).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        settableChannelFuture.setChannel(future.getChannel());
        if (future.isSuccess()) {
          socksConnect(future.getChannel(), (InetSocketAddress) remoteAddress)
            .addListener(new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture innerFuture) throws Exception {
                if (innerFuture.isSuccess()) {
                  settableChannelFuture.setSuccess();
                } else {
                  settableChannelFuture.setFailure(innerFuture.getCause());
                }
              }
            });
        } else {
          settableChannelFuture.setFailure(future.getCause());
        }
      }
    });
    return settableChannelFuture;
  }


  /**
   * try to look at the remoteAddress and decide to use SOCKS4 or SOCKS4a handshake
   * packet.
   *
   * @param channel
   * @param remoteAddress
   * @return
   */
  private ChannelFuture socksConnect(Channel channel, InetSocketAddress remoteAddress) {
    ChannelBuffer handshake = null;
    if ((remoteAddress.getAddress() == null && remoteAddress.getHostName() != null)
      || remoteAddress.getHostName().equals("localhost")
      ) {
      handshake = createSock4aPacket(remoteAddress.getHostName(), remoteAddress.getPort());
    }
    if (remoteAddress.getAddress() != null) {
      handshake = createSocks4packet(remoteAddress.getAddress(), remoteAddress.getPort());
    }
    if (handshake != null) {
      channel.write(handshake);
      return ((Socks4HandshakeHandler)channel.getPipeline().get("handshake")).getChannelFuture();
    }
    throw new IllegalArgumentException("Invalid Address " + remoteAddress);
  }
}
