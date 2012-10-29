/**
 * Copyright 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.nifty.client;

import io.airlift.units.Duration;
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
 * Netty Equivalent to a {@link org.apache.thrift.transport.TFramedTransport} over a TSocket.
 * <p/>
 * This is just for a proof-of-concept to show that it can be done.
 * <p/>
 * You should just use a TSocket for sync client.
 * <p/>
 * This already has a built in TFramedTransport. No need to wrap.
 */
public class TNiftyClientTransport extends TNiftyAsyncClientTransport
{

    private final ChannelBuffer readBuffer;
    private final Duration readTimeout;
    private final Lock lock = new ReentrantLock();
    @GuardedBy("lock")
    private final Condition condition = lock.newCondition();
    private boolean closed;
    private Throwable exception;

    public TNiftyClientTransport(Channel channel, Duration readTimeout)
    {
        super(channel);
        this.readTimeout = readTimeout;
        this.readBuffer = ChannelBuffers.dynamicBuffer(256);
        setListener(new TNiftyClientListener()
        {
            @Override
            public void onFrameRead(Channel c, ChannelBuffer buffer)
            {
                lock.lock();
                try {
                    readBuffer.discardReadBytes();
                    readBuffer.writeBytes(buffer);
                    condition.signal();
                }
                finally {
                    lock.unlock();
                }
            }

            @Override
            public void onChannelClosedOrDisconnected(Channel channel)
            {
                lock.lock();
                try {
                    closed = true;
                    condition.signal();
                }
                finally {
                    lock.unlock();
                }
            }

            @Override
            public void onExceptionEvent(ExceptionEvent e)
            {
                lock.lock();
                try {
                    exception = e.getCause();
                    condition.signal();
                }
                finally {
                    lock.unlock();
                }
            }
        });
    }

    @Override
    public int read(byte[] bytes, int offset, int length)
            throws TTransportException
    {
        try {
            return this.read(bytes, offset, length, readTimeout);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TTransportException(e);
        }
    }

    // yeah, mimicking sync with async is just horrible
    private int read(byte[] bytes, int offset, int length, Duration timeout)
            throws InterruptedException, TTransportException
    {
        long timeRemaining = (long)timeout.convertTo(TimeUnit.NANOSECONDS);
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
                    }
                    finally {
                        exception = null;
                        closed = true;
                        close();
                    }
                }
            }
        }
        finally {
            lock.unlock();
        }
        throw new TTransportException(String.format("read timeout, %d ms has elapsed",
                                                    (long)timeout.toMillis()));
    }
}
