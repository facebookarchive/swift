package com.facebook.nifty.client;

import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

/**
 * Netty Equivalent to a TFrameTransport over a TSocket.
 *
 * This is just for a proof-of-concept to show that it can be done.
 *
 * You should just use a TSocket for sync client.
 *
 * This already has a built in TFramedTransport. No need to wrap.
 */
public class TNiftyClientTransport extends TNiftyAsyncClientTransport {

  private final ChannelBuffer readBuffer;

  public TNiftyClientTransport(Channel channel) {
    super(channel);
    this.readBuffer = ChannelBuffers.dynamicBuffer(256);
    setListener(new TNiftyClientListener() {
      @Override
      public void onFrameRead(Channel c, ChannelBuffer buffer) {
        transferReadBuffer(buffer);
      }
    });
  }

  // yeah, mimicking sync with async is just horrible
  @Override
  public int read(byte[] bytes, int offset, int length) throws TTransportException {
    while (true) {
      synchronized (readBuffer) {
        int bytesAvailable = readBuffer.readableBytes();
        if (bytesAvailable > 0) {
          int begin = readBuffer.readerIndex();
          readBuffer.readBytes(bytes, offset, Math.min(bytesAvailable, length));
          int end = readBuffer.readerIndex();
          return end - begin;
        }
        try {
          readBuffer.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  // yeah, mimicking sync with async is just horrible
  void transferReadBuffer(ChannelBuffer incoming) {
    synchronized (readBuffer) {
      readBuffer.discardReadBytes();
      readBuffer.writeBytes(incoming);
      readBuffer.notify();
    }
  }
}
