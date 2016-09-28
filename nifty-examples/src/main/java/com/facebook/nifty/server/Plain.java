/*
 * Copyright (C) 2012-2016 Facebook, Inc.
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

import com.facebook.nifty.core.NettyServerConfig;
import com.facebook.nifty.core.NettyServerConfigBuilder;
import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.facebook.nifty.guice.NiftyModule;
import com.facebook.nifty.ssl.OpenSslServerConfiguration;
import com.facebook.nifty.test.LogEntry;
import com.facebook.nifty.test.ResultCode;
import com.facebook.nifty.test.scribe;
import com.google.inject.Guice;
import com.google.inject.Stage;
import io.airlift.log.Logger;
import org.apache.thrift.TException;
import org.apache.tomcat.jni.SessionTicketKey;

import javax.inject.Provider;

import java.io.File;
import java.security.SecureRandom;
import java.util.List;

/**
 * An example of how to create a Nifty server without plugging into config or lifecycle framework.
 */
public class Plain
{
    private static final Logger log = Logger.get(Plain.class);

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
                        SessionTicketKey[] keys = { createFakeSessionTicketKey() };
                        bind().toInstance(new ThriftServerDefBuilder()
                                        .listen(8080)
                                        .withProcessor(new scribe.Processor<scribe.Iface>(new scribe.Iface() {
                                            @Override
                                            public ResultCode Log(List<LogEntry> messages)
                                                    throws TException {
                                                for (LogEntry message : messages) {
                                                    log.info("%s: %s", message.getCategory(), message.getMessage());
                                                }
                                                return ResultCode.OK;
                                            }
                                        })).withSSLConfiguration(
                                                OpenSslServerConfiguration.newBuilder()
                                                        .certFile(new File(getClass().getResource("/rsa.crt").getFile()))
                                                        .keyFile(new File(getClass().getResource("/rsa.key").getFile()))
                                                        .allowPlaintext(true)
                                                        .ticketKeys(keys)
                                                        .sslVersion(OpenSslServerConfiguration.SSLVersion.TLS1_2)
                                                        .enableStatefulSessionCache(false)
                                                        .build())
                                        .build()
                        );
                        withNettyServerConfig(NettyConfigProvider.class);
                    }
                }
        ).getInstance(NiftyBootstrap.class);

        bootstrap.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                bootstrap.stop();
            }
        });
    }

    public static class NettyConfigProvider implements Provider<NettyServerConfig>
    {
        @Override
        public NettyServerConfig get()
        {
            NettyServerConfigBuilder nettyConfigBuilder = new NettyServerConfigBuilder();
            nettyConfigBuilder.getSocketChannelConfig().setTcpNoDelay(true);
            nettyConfigBuilder.getSocketChannelConfig().setConnectTimeoutMillis(5000);
            nettyConfigBuilder.getSocketChannelConfig().setTcpNoDelay(true);
            return nettyConfigBuilder.build();
        }
    }

    private static SessionTicketKey createFakeSessionTicketKey() {
        byte[] name = new byte[SessionTicketKey.NAME_SIZE];
        byte[] aesKey = new byte[SessionTicketKey.AES_KEY_SIZE];
        byte[] hmacKey = new byte[SessionTicketKey.HMAC_KEY_SIZE];

        SecureRandom random = new SecureRandom();
        random.nextBytes(name);
        random.nextBytes(aesKey);
        random.nextBytes(hmacKey);
        return new SessionTicketKey(name, aesKey, hmacKey);
    }
}
