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
package com.facebook.swift.service.async;

import com.facebook.swift.service.scribe.LogEntry;
import com.facebook.swift.service.scribe.scribe;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

public class HttpScribeServer implements AutoCloseable
{
    Server jettyServer;

    public void start() throws Exception
    {
        jettyServer = createServer();
    }

    public void close() throws Exception
    {
        shutdownServer(jettyServer);
    }

    public int getLocalPort()
    {
        return ((ServerConnector) jettyServer.getConnectors()[0]).getLocalPort();
    }

    public List<LogEntry> getLogEntries() throws ServletException
    {
        return getServerServlet(jettyServer).getLogEntries();
    }

    private Server createServer()
            throws Exception
    {
        Server httpServer = new Server();

        ServerConnector connector = new ServerConnector(httpServer);
        connector.setPort(0);
        httpServer.addConnector(connector);

        List<LogEntry> logEntries = new ArrayList<LogEntry>();
        com.facebook.swift.service.scribe.scribe.Iface handler = new TestThriftServletHandler(logEntries);
        TServlet servlet = new TestThriftServlet(handler, logEntries);

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/thrift");
        contextHandler.addServlet(new ServletHolder(servlet), "/*");

        httpServer.setHandler(contextHandler);

        httpServer.start();

        return httpServer;
    }

    private TestThriftServlet getServerServlet(Server server)
            throws ServletException
    {
        ServletContextHandler handler = (ServletContextHandler) server.getHandlers()[0];
        return (TestThriftServlet) handler.getServletHandler().getServlets()[0].getServlet();
    }

    private void shutdownServer(Server httpServer)
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
                scribe.Iface handler, List<com.facebook.swift.service.scribe.LogEntry> logEntries)
        {
            super(new scribe.Processor<>(handler), new TBinaryProtocol.Factory());
            this.logEntries = logEntries;
        }
    }
}
