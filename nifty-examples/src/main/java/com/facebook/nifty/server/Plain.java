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
package com.facebook.nifty.server;

import com.facebook.nifty.core.NettyConfigBuilder;
import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.facebook.nifty.guice.NiftyModule;
import com.facebook.nifty.test.LogEntry;
import com.facebook.nifty.test.ResultCode;
import com.facebook.nifty.test.scribe;
import com.google.inject.Guice;
import com.google.inject.Stage;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.util.List;

/**
 * An example of how to create a Nifty server without plugging into config or lifecycle framework.
 */
public class Plain
{
    private static final Logger log = LoggerFactory.getLogger(Plain.class);

    public static void main(String[] args)
            throws Exception
    {
        final NiftyBootstrap bootstrap = Guice.createInjector(
                Stage.PRODUCTION,
                new NiftyModule()
                {
                    @Override
                    protected void configureNifty()
                    {
                        bind().toInstance(new ThriftServerDefBuilder()
                                .listen(8080)
                                .withProcessor(new scribe.Processor<scribe.Iface>(new scribe.Iface()
                                {
                                    @Override
                                    public ResultCode Log(List<LogEntry> messages)
                                            throws TException
                                    {
                                        for (LogEntry message : messages) {
                                            log.info("{}: {}", message.getCategory(), message.getMessage());
                                        }
                                        return ResultCode.OK;
                                    }
                                }))
                                .build()
                        );
                        withNettyConfig(NettyConfigProvider.class);
                    }
                }
        ).getInstance(NiftyBootstrap.class);

        bootstrap.start();

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                bootstrap.stop();
            }
        });
    }

    public static class NettyConfigProvider implements Provider<NettyConfigBuilder>
    {
        @Override
        public NettyConfigBuilder get()
        {
            NettyConfigBuilder nettyConfigBuilder = new NettyConfigBuilder();
            nettyConfigBuilder.getSocketChannelConfig().setTcpNoDelay(true);
            nettyConfigBuilder.getSocketChannelConfig().setConnectTimeoutMillis(5000);
            nettyConfigBuilder.getSocketChannelConfig().setTcpNoDelay(true);
            return nettyConfigBuilder;
        }
    }
}
