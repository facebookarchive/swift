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
package com.facebook.swift.service.async;

import com.facebook.swift.service.LogEntry;
import com.facebook.swift.service.ResultCode;
import com.facebook.swift.service.Scribe;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.scribe.scribe;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletException;

import static org.testng.Assert.assertEquals;

public class AsyncHttpClient extends AsyncTestBase
{
    private Server httpServer = null;

    // Test a simple sync client call through HTTP (in this case, to a TServlet running under Jetty)
    @Test
    public void testHttpClient()
            throws Exception
    {
        clientManager = new ThriftClientManager();
        Server jettyServer = createServer();

        // Server was just started, it shouldn't have recorded any messages yet
        assertEquals(getServerServlet(jettyServer).getLogEntries().size(), 0);

        int serverPort = jettyServer.getConnectors()[0].getLocalPort();

        try (Scribe client = createHttpClient(Scribe.class, serverPort).get()) {
            client.log(Lists.newArrayList(new LogEntry("testCategory", "testMessage")));
        }

        // Blocking call completed, check that it was successful in logging a message.
        assertEquals(getServerServlet(jettyServer).getLogEntries().size(), 1);

        shutdownServer();
    }

    // Test a simple async client call to the same servlet
    @Test
    public void testHttpAsyncClient()
            throws Exception
    {
        clientManager = new ThriftClientManager();
        final CountDownLatch latch = new CountDownLatch(1);
        final Server jettyServer = createServer();

        // Server was just started, it shouldn't have recorded any messages yet
        assertEquals(getServerServlet(jettyServer).getLogEntries().size(), 0);

        int serverPort = jettyServer.getConnectors()[0].getLocalPort();

        ListenableFuture<AsyncScribe> clientFuture = createHttpClient(AsyncScribe.class, serverPort);
        Futures.addCallback(clientFuture, new FutureCallback<AsyncScribe>()
        {
            @Override
            public void onSuccess(AsyncScribe client)
            {
                try {
                    ListenableFuture<ResultCode> methodFuture =
                        client.log(Lists.newArrayList(new LogEntry("testCategory", "testMessage")));

                    // Connected a client, and made an async call against it, but the call shouldn't
                    // be completed right away. Check that it isn't.
                    assertEquals(getServerServlet(jettyServer).getLogEntries().size(), 0);

                    Futures.addCallback(methodFuture, new FutureCallback<ResultCode>()
                    {
                        @Override
                        public void onSuccess(ResultCode result)
                        {
                            latch.countDown();
                        }

                        @Override
                        public void onFailure(Throwable t)
                        {
                            latch.countDown();
                        }
                    });
                }
                catch (Throwable th) {
                    onFailure(th);
                }
            }

            @Override
            public void onFailure(Throwable t)
            {
                latch.countDown();
            }
        });

        latch.await();

        // Now that the latch is clear, client should have connected and the async call completed
        // so check that it did so successfully.
        assertEquals(getServerServlet(jettyServer).getLogEntries().size(), 1);

        shutdownServer();
    }

    private Server createServer()
            throws Exception
    {
        httpServer = new Server();

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(0);
        httpServer.addConnector(connector);

        List<com.facebook.swift.service.scribe.LogEntry> logEntries =
                new ArrayList<com.facebook.swift.service.scribe.LogEntry>();
        com.facebook.swift.service.scribe.scribe.Iface handler =
                new TestThriftServletHandler(logEntries);
        TServlet servlet = new TestThriftServlet(handler, logEntries);

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/thrift");
        contextHandler.addServlet(new ServletHolder(servlet), "/*");

        httpServer.setHandler(contextHandler);

        httpServer.start();

        return httpServer;
    }

    private int getServerPort(Server server)
    {
        return server.getConnectors()[0].getLocalPort();
    }

    private TestThriftServlet getServerServlet(Server server)
            throws ServletException
    {
        ServletContextHandler handler = (ServletContextHandler)server.getHandlers()[0];
        return (TestThriftServlet)handler.getServletHandler().getServlets()[0].getServlet();
    }

    private void shutdownServer()
            throws Exception
    {
        if (httpServer != null) {
            httpServer.stop();
            httpServer.join();
        }
    }

    private static class TestThriftServletHandler implements com.facebook.swift.service.scribe.scribe.Iface
    {
        private final List<com.facebook.swift.service.scribe.LogEntry> logEntries;

        private TestThriftServletHandler(List<com.facebook.swift.service.scribe.LogEntry> logEntries)
        {
            this.logEntries = logEntries;
        }

        @Override
        public com.facebook.swift.service.scribe.ResultCode Log(List<com.facebook.swift.service.scribe.LogEntry> messages)
                throws TException
        {
            try {
                Thread.sleep(100);
                logEntries.addAll(messages);
            }
            catch (InterruptedException e) {
                throw new TApplicationException(TApplicationException.UNKNOWN);
            }
            return com.facebook.swift.service.scribe.ResultCode.OK;
        }
    }

    private class TestThriftServlet extends TServlet
    {
        public List<com.facebook.swift.service.scribe.LogEntry> getLogEntries()
        {
            return logEntries;
        }

        private final List<com.facebook.swift.service.scribe.LogEntry> logEntries;

        public TestThriftServlet(
                scribe.Iface handler,
                List<com.facebook.swift.service.scribe.LogEntry> logEntries)
        {
            super(new scribe.Processor<>(handler), new TBinaryProtocol.Factory());
            this.logEntries = logEntries;
        }
    }
}
