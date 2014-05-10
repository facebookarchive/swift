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

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.core.NettyServerConfig;
import com.facebook.nifty.core.NettyServerConfigBuilder;
import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.core.RequestContexts;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.facebook.nifty.guice.NiftyModule;
import com.facebook.nifty.test.LogEntry;
import com.facebook.nifty.test.ResultCode;
import com.facebook.nifty.test.scribe;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Guice;
import com.google.inject.Provider;
import com.google.inject.Stage;
import io.airlift.units.Duration;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

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
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
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

    @Test
    public void testRejectedExecution()
            throws InterruptedException, TException
    {
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, queue);
        final TProcessor defaultProcessor = defaultProcessor();
        TProcessor slowProcessor = new TProcessor()
        {
            @Override
            public boolean process(
                    TProtocol in, TProtocol out)
                    throws TException
            {
                Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
                return defaultProcessor.process(in, out);
            }
        };
        startServer(getThriftServerDefBuilder().using(executor).withProcessor(slowProcessor));

        scribe.Client client1 = makeClient();
        scribe.Client client2 = makeClient();
        scribe.Client client3 = makeClient();

        List<LogEntry> messages = ImmutableList.of(new LogEntry("hello", "world"));

        client1.send_Log(messages);
        Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        client2.send_Log(messages);
        Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        client3.send_Log(messages);

        client1.recv_Log();
        client2.recv_Log();

        try {
            client3.recv_Log();
            fail("Third call should have failed because server queue is too short");
        }
        catch (TApplicationException ex)
        {
            // expected behavior:
            // - first call is not queued, goes directly to a worker thread on the executor
            // - second call is queued, filling the queue which has a fixed length of 1
            // - third call is rejected, we should receive an INTERNAL_ERROR
            assertEquals(ex.getType(), TApplicationException.INTERNAL_ERROR);
        }
    }

    private scribe.Client makeClient()
            throws TTransportException
    {
        TSocket socket = new TSocket("localhost", port);
        socket.open();
        socket.setTimeout(1000);
        TBinaryProtocol in = new TBinaryProtocol(new TFramedTransport(socket));
        TBinaryProtocol out = new TBinaryProtocol(new TFramedTransport(socket));
        return new scribe.Client(in, out);
    }

    private scribe.Client makeNiftyClient()
            throws TTransportException, InterruptedException
    {
        InetSocketAddress address = new InetSocketAddress("localhost", port);
        TTransport transport = new NiftyClient().connectSync(scribe.Client.class, new FramedClientConnector(address));
        TBinaryProtocol tp = new TBinaryProtocol(transport);
        return new scribe.Client(tp);
    }


    private void startServer()
    {
        startServer(getThriftServerDefBuilder());
    }

    private void startServer(final ThriftServerDefBuilder thriftServerDefBuilder)
    {
        bootstrap = Guice.createInjector(
                Stage.PRODUCTION,
                new NiftyModule()
                {
                    @Override
                    protected void configureNifty()
                    {
                        bind().toInstance(thriftServerDefBuilder.build());
                        withNettyServerConfig(new Provider<NettyServerConfig>()
                        {
                            @Override
                            public NettyServerConfig get()
                            {
                                return new NettyServerConfigBuilder().setWorkerThreadCount(1).build();
                            }
                        });
                    }
                }
        ).getInstance(NiftyBootstrap.class);

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
                .withProcessor(defaultProcessor());
    }

    private scribe.Processor<scribe.Iface> defaultProcessor()
    {
        return new scribe.Processor<scribe.Iface>(new scribe.Iface() {
            @Override
            public ResultCode Log(List<LogEntry> messages)
                    throws TException
            {
                RequestContext context = RequestContexts.getCurrentContext();

                for (LogEntry message : messages) {
                    log.info("[Client: {}] {}: {}",
                             context.getConnectionContext().getRemoteAddress(),
                             message.getCategory(),
                             message.getMessage());
                }
                return ResultCode.OK;
            }
        });
    }
}
