/*
 * Copyright (C) 2012 Facebook, Inc.
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
package com.facebook.nifty.server;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.Set;

public class DeafSelectorProvider extends SelectorProvider
{
    @Override
    public DatagramChannel openDatagramChannel() throws IOException
    {
        return null;
    }

    @Override
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException
    {
        return null;
    }

    @Override
    public Pipe openPipe() throws IOException
    {
        return null;
    }

    @Override
    public AbstractSelector openSelector() throws IOException
    {
        return new AbstractSelector(this) {

            private final Set<SelectionKey> keys = Sets.newHashSet();

            @Override
            protected void implCloseSelector() throws IOException
            {
            }

            @Override
            protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att)
            {
                final AbstractSelector selector = this;

                final SelectionKey key = new SelectionKey() {

                    @Override
                    public Selector selector()
                    {
                        return selector;
                    }

                    @Override
                    public int readyOps()
                    {
                        return 0;
                    }

                    @Override
                    public boolean isValid()
                    {
                        return true;
                    }

                    @Override
                    public SelectionKey interestOps(int ops)
                    {
                        return this;
                    }

                    @Override
                    public int interestOps()
                    {
                        return 0;
                    }

                    @Override
                    public SelectableChannel channel()
                    {
                        return null;
                    }

                    @Override
                    public void cancel()
                    {
                    }
                };
                key.interestOps(ops);
                key.attach(att);

                keys.add(key);
                return key;
            }


            @Override
            public Set<SelectionKey> keys()
            {
                return keys;
            }

            @Override
            public Set<SelectionKey> selectedKeys()
            {
                return Collections.emptySet();
            }

            @Override
            public int selectNow() throws IOException
            {
                return 0;
            }

            @Override
            public int select(long timeout) throws IOException
            {
                return 0;
            }

            @Override
            public int select() throws IOException
            {
                return 0;
            }

            @Override
            public Selector wakeup()
            {
                return null;
            }

        };
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException
    {
        return null;
    }

    @Override
    public SocketChannel openSocketChannel() throws IOException
    {
        return new SocketChannel(this) {

            @Override
            public SocketAddress getLocalAddress() throws IOException
            {
                return null;
            }

            @Override
            public <T> T getOption(SocketOption<T> name) throws IOException
            {
                return null;
            }

            @Override
            public Set<SocketOption<?>> supportedOptions()
            {
                return null;
            }

            @Override
            public SocketChannel bind(SocketAddress local) throws IOException
            {
                return null;
            }

            @Override
            public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException
            {
                return null;
            }

            @Override
            public SocketChannel shutdownInput() throws IOException
            {
                return null;
            }

            @Override
            public SocketChannel shutdownOutput() throws IOException
            {
                return null;
            }

            @Override
            public Socket socket()
            {
                return new Socket();
            }

            @Override
            public boolean isConnected()
            {
                return false;
            }

            @Override
            public boolean isConnectionPending()
            {
                return false;
            }

            @Override
            public boolean connect(SocketAddress remote) throws IOException
            {
                return false;
            }

            @Override
            public boolean finishConnect() throws IOException
            {
                return false;
            }

            @Override
            public SocketAddress getRemoteAddress() throws IOException
            {
                return null;
            }

            @Override
            public int read(ByteBuffer dst) throws IOException
            {
                return 0;
            }

            @Override
            public long read(ByteBuffer[] dsts, int offset, int length) throws IOException
            {
                return 0;
            }

            @Override
            public int write(ByteBuffer src) throws IOException
            {
                return 0;
            }

            @Override
            public long write(ByteBuffer[] srcs, int offset, int length) throws IOException
            {
                return 0;
            }

            @Override
            protected void implCloseSelectableChannel() throws IOException
            {
            }

            @Override
            protected void implConfigureBlocking(boolean block) throws IOException
            {
            }
        };
    }
}

