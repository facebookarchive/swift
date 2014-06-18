/*
 * Copyright (C) 2012-2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.nifty.core;

import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * Composes a read-only transport and a write-only transport to produce a read/write transport
 */
public class TInputOutputCompositeTransport extends TTransport
{
    private final TTransport inputTransport;
    private final TTransport outputTransport;

    public TInputOutputCompositeTransport(TTransport inputTransport, TTransport outputTransport)
    {
        this.inputTransport = inputTransport;
        this.outputTransport = outputTransport;
    }

    // Delegate input methods

    @Override
    public int read(byte[] buf, int off, int len) throws TTransportException
    {
        return inputTransport.read(buf, off, len);
    }

    @Override
    public int readAll(byte[] buf, int off, int len) throws TTransportException
    {
        return inputTransport.readAll(buf, off, len);
    }

    @Override
    public boolean peek()
    {
        return inputTransport.peek();
    }

    @Override
    public byte[] getBuffer()
    {
        return inputTransport.getBuffer();
    }

    @Override
    public int getBufferPosition()
    {
        return inputTransport.getBufferPosition();
    }

    @Override
    public int getBytesRemainingInBuffer()
    {
        return inputTransport.getBytesRemainingInBuffer();
    }

    @Override
    public void consumeBuffer(int len)
    {
        inputTransport.consumeBuffer(len);
    }

    // Delegate output methods

    @Override
    public void write(byte[] buf) throws TTransportException
    {
        outputTransport.write(buf);
    }

    @Override
    public void write(byte[] buf, int off, int len) throws TTransportException
    {
        outputTransport.write(buf, off, len);
    }

    @Override
    public void flush() throws TTransportException
    {
        outputTransport.flush();
    }

    // Unsupported methods

    @Override
    public void close()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void open() throws TTransportException
    {
        throw new UnsupportedOperationException();
    }
}
