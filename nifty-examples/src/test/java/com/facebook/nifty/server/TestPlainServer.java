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
import io.airlift.units.Duration;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestPlainServer
{

    private static final Logger log = LoggerFactory.getLogger(TestPlainServer.class);

    public static final String VERSION = "1.0";
    private NiftyBootstrap bootstrap;
    private int port;

    @BeforeMethod(alwaysRun = true)
    public void setup()
    {
        bootstrap = null;
    }

    @AfterMethod(alwaysRun = true)
    public void teardown()
            throws InterruptedException
    {
        if (bootstrap != null) {
            bootstrap.stop();
        }
    }

    @Test
    public void testMethodCalls()
            throws Exception
    {
        startServer();
        scribe.Client client = makeClient();
        client.Log(Arrays.asList(new LogEntry("hello", "world")));
    }

    @Test
    public void testMethodCallsWithNiftyClient()
            throws Exception
    {
        startServer();
        scribe.Client client = makeNiftyClient();
        int max = (int) (Math.random() * 100);
        for (int i = 0; i < max; i++) {
            client.Log(Arrays.asList(new LogEntry("hello", "world " + i)));
        }
    }

    @Test
    public void testDefaultConnectionIdleTimeout()
            throws InterruptedException, TException
    {
        startServer(getThriftServerDefBuilder());
        scribe.Client client = makeNiftyClient();
        Thread.sleep(500);
        client.Log(Arrays.asList(new LogEntry("hello", "world")));
    }

    @Test(expectedExceptions = { TTransportException.class })
    public void testSpecifiedConnectionIdleTimeout()
            throws InterruptedException, TException
    {
        startServer(getThriftServerDefBuilder().clientIdleTimeout(new Duration(250, TimeUnit.MILLISECONDS)));
        scribe.Client client = makeNiftyClient();
        Thread.sleep(500);
        client.Log(Arrays.asList(new LogEntry("hello", "world")));
    }

    private scribe.Client makeClient()
            throws TTransportException
    {
        TSocket socket = new TSocket("localhost", port);
        socket.open();
        TBinaryProtocol tp = new TBinaryProtocol(new TFramedTransport(socket));
        return new scribe.Client(tp);
    }

    private scribe.Client makeNiftyClient()
            throws TTransportException, InterruptedException
    {
        InetSocketAddress address = new InetSocketAddress("localhost", port);
        TBinaryProtocol tp = new TBinaryProtocol(new NiftyClient().connectSync(address));
        return new scribe.Client(tp);
    }


    private void startServer()
    {
        startServer(getThriftServerDefBuilder());
    }

    private void startServer(final ThriftServerDefBuilder thriftServerDefBuilder)
    {
        bootstrap = Guice.createInjector(Stage.PRODUCTION, new NiftyModule() {
            @Override
            protected void configureNifty() {
                bind().toInstance(thriftServerDefBuilder.build());
            }
        }).getInstance(NiftyBootstrap.class);

        bootstrap.start();
    }

    private ThriftServerDefBuilder getThriftServerDefBuilder()
    {
        try {
            ServerSocket s = new ServerSocket();
            s.bind(new InetSocketAddress(0));
            port = s.getLocalPort();
            s.close();
        }
        catch (IOException e) {
            port = 8080;
        }

        return new ThriftServerDefBuilder()
                .listen(port)
                .withProcessor(new scribe.Processor<>(new scribe.Iface() {
                    @Override
                    public ResultCode Log(List<LogEntry> messages)
                            throws TException {
                        for (LogEntry message : messages) {
                            log.info("{}: {}", message.getCategory(),
                                     message.getMessage());
                        }
                        return ResultCode.OK;
                    }
                }));
    }
}
