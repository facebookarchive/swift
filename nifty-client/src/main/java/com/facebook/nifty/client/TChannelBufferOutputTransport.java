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

import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Implementation of {@link TTransport} that buffers the output of a single message,
 * so that an async client can grab the buffer and send it using a {@link NiftyClientChannel}.
 */
@NotThreadSafe
public class TChannelBufferOutputTransport extends TTransport {
    private final ChannelBuffer outputBuffer = ChannelBuffers.dynamicBuffer(1024);

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void open() throws TTransportException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(byte[] buf, int off, int len) throws TTransportException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(byte[] buf, int off, int len) throws TTransportException {
        outputBuffer.writeBytes(buf, off, len);
    }

    public ChannelBuffer getOutputBuffer() {
        return outputBuffer;
    }
}
