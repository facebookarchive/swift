package com.facebook.nifty.client;

import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ExceptionEvent;

import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Netty Equivalent to a TFrameTransport over a TSocket.
 * <p/>
 * This is just for a proof-of-concept to show that it can be done.
 * <p/>
 * You should just use a TSocket for sync client.
 * <p/>
 * This already has a built in TFramedTransport. No need to wrap.
 */
public class TNiftyClientTransport extends TNiftyAsyncClientTransport {

  private final ChannelBuffer readBuffer;
  private final long readTimeout;
  private final TimeUnit unit;
  private final Lock lock = new ReentrantLock();
  @GuardedBy("lock")
  private final Condition condition = lock.newCondition();
  private boolean closed = false;
  private Throwable exception = null;

  public TNiftyClientTransport(Channel channel, long readTimeout, TimeUnit unit) {
    super(channel);
    this.readTimeout = readTimeout;
    this.unit = unit;
    this.readBuffer = ChannelBuffers.dynamicBuffer(256);
    setListener(new TNiftyClientListener() {
      @Override
      public void onFrameRead(Channel c, ChannelBuffer buffer) {
        lock.lock();
        try {
          readBuffer.discardReadBytes();
          readBuffer.writeBytes(buffer);
          condition.signal();
        } finally {
          lock.unlock();
        }
      }

      @Override
      public void onChannelClosedOrDisconnected(Channel channel) {
        lock.lock();
        try {
          closed = true;
          condition.signal();
        } finally {
          lock.unlock();
        }
      }

      @Override
      public void onExceptionEvent(ExceptionEvent e) {
        lock.lock();
        try {
          exception = e.getCause();
          condition.signal();
        } finally {
          lock.unlock();
        }
      }
    });
  }

  @Override
  public int read(byte[] bytes, int offset, int length) throws TTransportException {
    try {
      return this.read(bytes, offset, length, readTimeout, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TTransportException(e);
    }
  }

  // yeah, mimicking sync with async is just horrible
  private int read(byte[] bytes, int offset, int length, long timeout, TimeUnit unit) throws InterruptedException, TTransportException {
    long timeRemaining = unit.toNanos(timeout);
    lock.lock();
    try {
      while (true) {
        int bytesAvailable = readBuffer.readableBytes();
        if (bytesAvailable > 0) {
          int begin = readBuffer.readerIndex();
          readBuffer.readBytes(bytes, offset, Math.min(bytesAvailable, length));
          int end = readBuffer.readerIndex();
          return end - begin;
        }
        if (timeRemaining <= 0) {
          break;
        }
        timeRemaining = condition.awaitNanos(timeRemaining);
        if (closed) {
          throw new TTransportException("channel closed !");
        }
        if (exception != null) {
          try {
            throw new TTransportException(exception);
          } finally {
            exception = null;
            closed = true;
            close();
          }
        }
      }
    } finally {
      lock.unlock();
    }
    throw new TTransportException(String.format("read timeout, %d ms has elapsed", unit.toMillis(timeout)));
  }
}
