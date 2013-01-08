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

import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.facebook.nifty.guice.NiftyModule;
import com.facebook.nifty.test.LogEntry;
import com.facebook.nifty.test.ResultCode;
import com.facebook.nifty.test.scribe;
import com.google.inject.Guice;
import com.google.inject.Stage;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

public class TestNiftyClient
{
    private static final Logger log = LoggerFactory.getLogger(TestNiftyClient.class);

    private NiftyBootstrap bootstrap;
    private int port;

    @BeforeMethod(alwaysRun = true)
    public void setup() throws IOException
    {
        ServerSocket s = new ServerSocket();
        s.bind(new InetSocketAddress(0));
        port = s.getLocalPort();
        s.close();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown()
    {
        if (bootstrap != null)
        {
            bootstrap.stop();
        }
    }

    @Test
    public void testServerDisconnect()
            throws Exception
    {
        startServer();
        final NiftyClient niftyClient = new NiftyClient();
        scribe.Client client = makeNiftyClient(niftyClient);
        new Thread()
        {
            @Override
            public void run()
            {
                try {
                    sleep(1000L);
                    bootstrap.stop();
                }
                catch (InterruptedException e) {
                }
            }
        }.start();

        int max = (int) (Math.random() * 100) + 10;
        int exceptionCount = 0;
        for (int i = 0; i < max; i++) {
            Thread.sleep(100L);
            try {
                client.Log(Arrays.asList(new LogEntry("hello", "world " + i)));
            }
            catch (TException e) {
                log.info("caught expected exception " + e.toString());
                exceptionCount++;
            }
        }
        Assert.assertTrue(exceptionCount > 0);

        niftyClient.close();
    }

    private void startServer()
    {
        bootstrap = Guice.createInjector(
                Stage.PRODUCTION,
                new NiftyModule()
                {
                    @Override
                    protected void configureNifty()
                    {
                        bind().toInstance(new ThriftServerDefBuilder()
                                .listen(port)
                                .withProcessor(new scribe.Processor(new scribe.Iface()
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
                                .build());
                    }
                }
        ).getInstance(NiftyBootstrap.class);

        bootstrap.start();
    }

    private scribe.Client makeNiftyClient(final NiftyClient niftyClient)
            throws TTransportException, InterruptedException
    {
        InetSocketAddress address = new InetSocketAddress("localhost", port);
        TBinaryProtocol tp = new TBinaryProtocol(niftyClient.connectSync(address));
        return new scribe.Client(tp);
    }
}
