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
package com.facebook.nifty.server.util;

import com.facebook.nifty.core.NettyConfigBuilder;
import com.facebook.nifty.core.NettyServerTransport;
import com.facebook.nifty.core.ThriftServerDef;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import org.apache.thrift.TProcessor;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.util.HashedWheelTimer;

import java.net.InetSocketAddress;

public class ScopedNiftyServer implements AutoCloseable {
    private final NettyServerTransport server;

    public ScopedNiftyServer(ThriftServerDefBuilder defBuilder) {
        NettyConfigBuilder configBuilder = new NettyConfigBuilder();

        ThriftServerDef def = defBuilder.build();

        server = new NettyServerTransport(def,
                                          configBuilder,
                                          new DefaultChannelGroup(),
                                          new HashedWheelTimer());

        server.start();
    }

    public int getPort() {
        InetSocketAddress localAddress = (InetSocketAddress)server.getServerChannel()
                                                                  .getLocalAddress();
        return localAddress.getPort();
    }

    @Override
    public void close() throws Exception {
        server.stop();
    }

    public static ThriftServerDefBuilder defaultServerDefBuilder(TProcessor processor) {
        return ThriftServerDef.newBuilder()
                              .listen(0)
                              .withProcessor(processor);
    }
}
