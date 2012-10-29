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
package com.facebook.nifty.server;

import com.facebook.nifty.client.FramedClientChannel;
import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.facebook.nifty.guice.NiftyModule;
import com.facebook.nifty.test.LogEntry;
import com.facebook.nifty.test.ResultCode;
import com.facebook.nifty.test.scribe;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Guice;
import com.google.inject.Stage;
import io.airlift.units.Duration;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class TestNiftyClient
{
    private static final Logger log = LoggerFactory.getLogger(TestNiftyClient.class);

    private NiftyBootstrap bootstrap;
    private int port;
    private static final Duration TEST_CONNECT_TIMEOUT = new Duration(500, TimeUnit.MILLISECONDS);
    private static final Duration TEST_READ_TIMEOUT = new Duration(500, TimeUnit.MILLISECONDS);
    private static final Duration TEST_WRITE_TIMEOUT = new Duration(500, TimeUnit.MILLISECONDS);

    @BeforeTest(alwaysRun = true)
    public void setup()
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
    }

    @AfterTest(alwaysRun = true)
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
        scribe.Client client = makeNiftyClient();
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
    }

    @Test(timeOut = 2000)
    public void testSyncConnectTimeout() throws ConnectException, IOException
    {
        ServerSocket serverSocket = createFloodedServerSocket();
        int port = serverSocket.getLocalPort();

        try {
            TTransport transport = new NiftyClient().connectSync(new InetSocketAddress(port),
                                                                 TEST_CONNECT_TIMEOUT,
                                                                 TEST_READ_TIMEOUT,
                                                                 TEST_WRITE_TIMEOUT);
        }
        catch (Throwable throwable) {
            if (isTimeoutException(throwable)) {
                return;
            }
            Throwables.propagate(throwable);
        }
        finally {
            serverSocket.close();
        }

        // Should never get here
        fail("Connection succeeded but failure was expected");
    }

    @Test(timeOut = 2000)
    public void testAsyncConnectTimeout() throws IOException
    {
        ServerSocket serverSocket = createFloodedServerSocket();
        int port = serverSocket.getLocalPort();

        try {
            ListenableFuture<FramedClientChannel> future =
                    new NiftyClient().connectAsync(new FramedClientChannel.Factory(),
                                                   new InetSocketAddress(port),
                                                   TEST_CONNECT_TIMEOUT,
                                                   TEST_READ_TIMEOUT,
                                                   TEST_WRITE_TIMEOUT);
            // Wait while NiftyClient attempts to connect the channel
            NiftyClientChannel channel = future.get();
        }
        catch (Throwable throwable) {
            if (isTimeoutException(throwable)) {
                return;
            }
            Throwables.propagate(throwable);
        }
        finally {
            serverSocket.close();
        }

        // Should never get here
        fail("Connection succeeded but failure was expected");
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable rootCause = Throwables.getRootCause(throwable);
        // Look for a java.net.ConnectException, with the message "connection timed out"
        return (rootCause instanceof ConnectException &&
                rootCause.getMessage().compareTo("connection timed out") == 0);
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

    private scribe.Client makeNiftyClient()
            throws TTransportException, InterruptedException
    {
        InetSocketAddress address = new InetSocketAddress("localhost", port);
        TBinaryProtocol tp = new TBinaryProtocol(new NiftyClient().connectSync(address));
        return new scribe.Client(tp);
    }

    private ServerSocket createFloodedServerSocket() throws IOException {
        // Setup a server socket that with a backlog of only one connection. NOTE: behavior
        // of java's ServerSocket backlog is declared to be implementation specific according to
        // JDK docs, but this is the only way I've found so far to simulate a connect timeout that
        // works regardless of the presence/absence of an internet connection.
        ServerSocket serverSocket = new ServerSocket(0, 1);

        // Connect a client to the socket. Since we don't call accept(), this should fill
        // the backlog, making further connect attempts timeout.
        Socket client = new Socket();
        client.connect(new InetSocketAddress(serverSocket.getLocalPort()));

        return serverSocket;
    }
}
