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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.transport.TIOStreamTransport;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import com.facebook.nifty.processor.NiftyProcessor;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

public class AbstractLiveTest
{
    protected AbstractLiveTest() { }


    protected FakeServer listen(NiftyProcessor processor) {
        // NiftyBootstrap.stop() will shutdown the threadpool for us
        return new FakeServer(processor, Executors.newCachedThreadPool(), new DefaultChannelGroup());
    }

    protected FakeServer listen(NiftyProcessor processor, Executor taskExecutor, ChannelGroup group) {
        return new FakeServer(processor, taskExecutor, group);
    }

    protected FakeClient connect(FakeServer server) throws IOException {
        return new FakeClient(server);
    }

    protected NiftyProcessor mockProcessor(
            @Nullable final BlockingQueue<TProtocol> inQueue,
            @Nullable final BlockingQueue<TProtocol> outQueue,
            @Nullable final BlockingQueue<RequestContext> requestContextQueue,
            @Nonnull final BlockingQueue<SettableFuture<Boolean>> responseQueue
    ) {
        return new NiftyProcessor() {
            @Override
            public ListenableFuture<Boolean> process(TProtocol in, TProtocol out,
                            RequestContext requestContext) throws TException {
                if (inQueue != null) {
                    Uninterruptibles.putUninterruptibly(inQueue, in);
                }
                if (outQueue != null) {
                    Uninterruptibles.putUninterruptibly(outQueue, out);
                }
                if (requestContextQueue != null) {
                    Uninterruptibles.putUninterruptibly(requestContextQueue, requestContext);
                }
                SettableFuture<Boolean> resp = SettableFuture.create();
                Uninterruptibles.putUninterruptibly(responseQueue, resp);
                return resp;
            }
        };
    }


    protected static class FakeServer implements AutoCloseable {
        private final NiftyBootstrap nifty;

        private FakeServer(NiftyProcessor processor, Executor taskExecutor, ChannelGroup group) {
            ThriftServerDef thriftServerDef =
                new ThriftServerDefBuilder()
                .withProcessor(processor)
                .using(taskExecutor)
                .build();

            this.nifty = new NiftyBootstrap(
                            ImmutableSet.of(thriftServerDef),
                            new NettyServerConfigBuilder().build(),
                            group);

            nifty.start();
        }

        public int getPort() {
            return Iterables.getOnlyElement(nifty.getBoundPorts().values());
        }

        @Override
        public void close() {
            nifty.stop();
        }
    }

    protected static class FakeClient implements AutoCloseable {
        private Socket socketToServer;

        private FakeClient(FakeServer server) throws IOException {
            socketToServer = new Socket(InetAddress.getLoopbackAddress(), server.getPort());
        }

        public int getClientPort() {
            return socketToServer.getLocalPort();
        }

        public void sendRequest() throws IOException, TException {
            TProtocol out = new TBinaryProtocol(new TIOStreamTransport(socketToServer.getOutputStream()));
            out.writeMessageBegin(new TMessage("dummy", TMessageType.CALL, 0));
            out.writeStructBegin(new TStruct("dummy_args"));
            out.writeFieldStop();
            out.writeStructEnd();
            out.writeMessageEnd();
            out.getTransport().flush();
        }

        @Override
        public void close() throws IOException {
            socketToServer.close();
            socketToServer = null;
        }
    }
}
