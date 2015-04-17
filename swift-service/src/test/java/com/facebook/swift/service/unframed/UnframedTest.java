/*
 * Copyright (C) 2012 Facebook, Inc.
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
package com.facebook.swift.service.unframed;

import com.facebook.nifty.client.UnframedClientConnector;
import com.facebook.swift.service.LogEntry;
import com.facebook.swift.service.ResultCode;
import com.facebook.swift.service.Scribe;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.async.AsyncScribe;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.airlift.log.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.testng.Assert.assertEquals;

public class UnframedTest
{
    private static final Logger LOG = Logger.get(UnframedTest.class);

    @Test
    public void testUnframedSyncMethod()
            throws Exception
    {
        TestServerInfo info = startServer();
        ThriftClientManager clientManager = new ThriftClientManager();

        try (Scribe client = createUnframedClient(clientManager, Scribe.class, info.port).get()) {
            ResultCode result = client.log(Lists.newArrayList(
                    new LogEntry("testCategory", "testMessage")));
            assertEquals(result, ResultCode.OK);
        }

        stopServer(info);
    }

    @Test
    public void testUnframedAsyncMethod()
            throws Exception
    {
        TestServerInfo info = startServer();
        ThriftClientManager clientManager = new ThriftClientManager();
        final CountDownLatch latch = new CountDownLatch(1);
        final ResultCode[] resultHolder = new ResultCode[1];

        ListenableFuture<AsyncScribe> clientFuture = createUnframedClient(clientManager, AsyncScribe.class, info.port);
        Futures.addCallback(clientFuture, new FutureCallback<AsyncScribe>()
        {
            @Override
            public void onSuccess(AsyncScribe client)
            {
                try {
                    ListenableFuture<ResultCode> methodFuture =
                            client.log(Lists.newArrayList(new LogEntry("testCategory", "testMessage")));
                    Futures.addCallback(methodFuture, new FutureCallback<ResultCode>()
                    {
                        @Override
                        public void onSuccess(ResultCode result)
                        {
                            resultHolder[0] = result;
                            latch.countDown();
                        }

                        @Override
                        public void onFailure(Throwable t)
                        {
                            latch.countDown();
                        }
                    });
                }
                catch (TException e) {
                    latch.countDown();
                }
            }

            @Override
            public void onFailure(Throwable t)
            {
                latch.countDown();
            }
        });

        latch.await();

        stopServer(info);
    }

    private <T> ListenableFuture<T> createUnframedClient(
            ThriftClientManager clientManager,
            Class<T> clientType,
            int servicePort)
            throws Exception
    {
        return clientManager.createClient(
                new UnframedClientConnector(HostAndPort.fromParts("localhost", servicePort)),
                clientType);
    }

    public TestServerInfo startServer()
            throws Exception
    {
        final TestServerInfo info = new TestServerInfo();
        TServerSocket serverSocket = new TServerSocket(0);
        com.facebook.swift.service.scribe.scribe.Iface handler = new PlainScribeHandler();
        TProcessor processor = new com.facebook.swift.service.scribe.scribe.Processor<>(handler);

        TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverSocket).processor(processor);
        final TServer thriftServer = info.server = new TThreadPoolServer(args);

        LOG.info("Server running on port %s", serverSocket.getServerSocket().getLocalPort());

        new Thread() {
            @Override
            public void run()
            {
                thriftServer.serve();
            }
        }.start();

        while (!info.server.isServing()) {
            Thread.sleep(10);
        }
        info.port = serverSocket.getServerSocket().getLocalPort();

        return info;
    }

    public void stopServer(TestServerInfo info)
            throws Exception
    {
        info.server.stop();
    }

    private class PlainScribeHandler implements com.facebook.swift.service.scribe.scribe.Iface
    {
        @Override
        public com.facebook.swift.service.scribe.ResultCode
            Log(List<com.facebook.swift.service.scribe.LogEntry> messages)
                throws TException
        {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                return com.facebook.swift.service.scribe.ResultCode.TRY_LATER;
            }
            return com.facebook.swift.service.scribe.ResultCode.OK;
        }
    }

    private class TestServerInfo
    {
        public TServer server;
        public int port;
    }
}
