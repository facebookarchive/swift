package com.facebook.nifty.client;

import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This already has a built in TFramedTransport. No need to wrap.
 */
@NotThreadSafe
public class TNiftyAsyncClientTransport extends TTransport implements ChannelUpstreamHandler, ChannelDownstreamHandler {
  private static final int DEFAULT_BUFFER_SIZE = 1024;
  // this is largely a guess. there shouldn't really be more than 2 write buffers at any given time.
  private static final int MAX_BUFFERS_IN_QUEUE = 3;
  private final Channel channel;
  private final Queue<ChannelBuffer> writeBuffers;
  private volatile TNiftyClientListener listener;

  public TNiftyAsyncClientTransport(Channel channel) {
    this.channel = channel;
    this.writeBuffers = new ConcurrentLinkedQueue<ChannelBuffer>();
  }

  public void setListener(TNiftyClientListener listener) {
    this.listener = listener;
  }

  @Override
  public boolean isOpen() {
    return channel.isOpen();
  }

  @Override
  public void open() throws TTransportException {
    // no-op
  }

  @Override
  public void close() {
    channel.close();
  }

  @Override
  public int read(byte[] bytes, int offset, int length) throws TTransportException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(byte[] bytes, int offset, int length) throws TTransportException {
    getWriteBuffer().writeBytes(bytes, offset, length);
  }

  @Override
  public void flush() throws TTransportException {
    // all these is to re-use the write buffer. We can only clear
    // and re-use a write buffer when the write operation completes,
    // which is an async operation in netty. the future listener
    // down here will be invoked by Netty I/O thread.
    if (!writeBuffers.isEmpty()) {
      final ChannelBuffer channelBuffer = writeBuffers.remove();
      channel.write(channelBuffer).addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (future.isSuccess()) {
            channelBuffer.clear();
            if (writeBuffers.size() < MAX_BUFFERS_IN_QUEUE) {
              writeBuffers.add(channelBuffer);
            }
          }
        }
      });
    }
  }

  @Override
  public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
    if (e instanceof MessageEvent) {
      messageReceived(ctx, (MessageEvent) e);
    } else if (e instanceof ChannelStateEvent) {
      ChannelStateEvent evt = (ChannelStateEvent) e;
      switch (evt.getState()) {
        case OPEN:
          if (Boolean.FALSE.equals(evt.getValue())) {
            listener.onChannelClosedOrDisconnected(ctx.getChannel());
          }
          break;
        case CONNECTED:
          if (evt.getValue() == null) {
            listener.onChannelClosedOrDisconnected(ctx.getChannel());
          }
          break;
      }
    } else if (e instanceof ExceptionEvent) {
      listener.onExceptionEvent((ExceptionEvent)e);
    }
    ctx.sendUpstream(e);
    // for all other stuff we drop it on the floor
  }

  private void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
    if (e.getMessage() instanceof ChannelBuffer) {
      if (listener != null) {
        listener.onFrameRead(ctx.getChannel(), (ChannelBuffer) e.getMessage());
      }
    }
    // drop it
  }

  @Override
  public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
    ctx.sendDownstream(e);
  }

  public ChannelBuffer getWriteBuffer() {
    if (writeBuffers.isEmpty()) {
      writeBuffers.add(ChannelBuffers.dynamicBuffer(DEFAULT_BUFFER_SIZE));
    }
    return writeBuffers.peek();
  }
}
