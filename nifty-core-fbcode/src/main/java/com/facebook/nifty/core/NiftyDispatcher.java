package com.facebook.nifty.core;

import org.apache.thrift.TException;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TConnectionContext;
import org.apache.thrift.transport.TTransport;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

/**
 * Dispatch TNiftyTransport to the TProcessor and write output back.
 *
 */
public class NiftyDispatcher extends SimpleChannelUpstreamHandler {

  private static final Logger log = LoggerFactory.getLogger(NiftyDispatcher.class);

  private final TProcessorFactory processorFactory;
  private final TProtocolFactory inProtocolFactory;
  private final TProtocolFactory outProtocolFactory;
  private final Executor exe;

  public NiftyDispatcher(ThriftServerDef def) {
    this.processorFactory = def.getProcessorFactory();
    this.inProtocolFactory = def.getInProtocolFactory();
    this.outProtocolFactory = def.getOutProtocolFactory();
    this.exe = def.getExecutor();
  }

  @Override
  public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
    throws Exception {
    if (e.getMessage() instanceof TTransport) {
      exe.execute(
        new Runnable() {
          @Override
          public void run() {
            TTransport t = (TTransport) e.getMessage();
            TProtocol inProtocol = inProtocolFactory.getProtocol(t);
            TProtocol outProtocol = outProtocolFactory.getProtocol(t);
            try {
              processorFactory.getProcessor(t).process(
                inProtocol,
                outProtocol,
                new TConnectionContext(inProtocol, outProtocol) {
                  @Override
                  public InetAddress getPeerAddress() {
                    return ((InetSocketAddress) ctx.getChannel().getRemoteAddress()).getAddress();
                  }
                }
              );
            } catch (TException e1) {
              log.error("Exception while invoking!", e1);
              closeChannel(ctx);
            }

          }
        }
      );
    } else {
      ctx.sendUpstream(e);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    // Any out of band exception are caught here and we tear down the socket
    closeChannel(ctx);
  }

  private void closeChannel(ChannelHandlerContext ctx) {
    if (ctx.getChannel().isOpen()) {
      ctx.getChannel().close();
    }
  }
}
