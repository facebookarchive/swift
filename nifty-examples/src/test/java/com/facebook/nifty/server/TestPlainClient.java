/*
 * Copyright (C) 2012 Facebook, Inc.
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
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

public class TestPlainClient {
    private NiftyBootstrap bootstrap;
    private static Logger log = LoggerFactory.getLogger(TestPlainClient.class);
    int port;

    @Test
    public void testPlainUnframedClient() throws TException {
        TSocket socket = new TSocket("localhost", port);
        socket.open();
        socket.setTimeout(1000);
        TBinaryProtocol protocol = new TBinaryProtocol(socket);

        scribe.Client client = new scribe.Client(protocol);

        LogEntry entry = new LogEntry("TestLog", "Test message from plain unframed client");
        client.Log(Arrays.asList(entry));

        socket.close();
    }

    @Test
    public void testPlainFramedClient() throws TException {
        TSocket socket = new TSocket("localhost", port);
        socket.open();
        socket.setTimeout(1000);
        TFramedTransport framedTransport = new TFramedTransport(socket);
        TBinaryProtocol protocol = new TBinaryProtocol(framedTransport);

        scribe.Client client = new scribe.Client(protocol);

        LogEntry entry = new LogEntry("TestLog", "Test message from plain framed client");
        client.Log(Arrays.asList(entry));

        socket.close();
    }

    @BeforeSuite
    public void startNiftyServer() throws IOException {
        ServerSocket s = new ServerSocket();
        s.bind(new InetSocketAddress(0));
        port = s.getLocalPort();
        s.close();

        bootstrap = Guice.createInjector(Stage.PRODUCTION, new NiftyModule() {
            @Override
            protected void configureNifty() {
                ThriftServerDefBuilder serverDefBuilder;

                serverDefBuilder = new ThriftServerDefBuilder()
                        .listen(port)
                        .withProcessor(new scribe.Processor(new scribe.Iface() {
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

                bind().toInstance(serverDefBuilder.build());
            }
        }).getInstance(NiftyBootstrap.class);

        bootstrap.start();
    }

    @AfterSuite
    private void stopNiftyServer() {
        bootstrap.stop();
    }
}
