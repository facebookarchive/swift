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

import com.facebook.nifty.server.util.ScopedNiftyServer;
import com.facebook.nifty.test.LogEntry;
import com.facebook.nifty.test.ResultCode;
import com.facebook.nifty.test.scribe;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.facebook.nifty.server.util.ScopedNiftyServer.defaultServerDefBuilder;

public class TestPlainClient {
    private static Logger log = LoggerFactory.getLogger(TestPlainClient.class);

    @BeforeMethod(alwaysRun = true)
    public void setup()
    {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
    }

    @Test
    public void testPlainUnframedClient() throws Exception
    {
        try (ScopedNiftyServer server = makeServer()) {
            TSocket socket = new TSocket("localhost", server.getPort());
            socket.open();
            socket.setTimeout(1000);
            TBinaryProtocol protocol = new TBinaryProtocol(socket);

            scribe.Client client = new scribe.Client(protocol);

            LogEntry entry = new LogEntry("TestLog", "Test message from plain unframed client");
            client.Log(Arrays.asList(entry));

            socket.close();
        }
    }

    @Test
    public void testPlainFramedClient() throws Exception {
        try (ScopedNiftyServer server = makeServer()) {
            TSocket socket = new TSocket("localhost", server.getPort());
            socket.open();
            socket.setTimeout(1000);
            TFramedTransport framedTransport = new TFramedTransport(socket);
            TBinaryProtocol protocol = new TBinaryProtocol(framedTransport);

            scribe.Client client = new scribe.Client(protocol);

            LogEntry entry = new LogEntry("TestLog", "Test message from plain framed client");
            client.Log(Arrays.asList(entry));

            socket.close();
        }
    }

    public ScopedNiftyServer makeServer() throws IOException {
        TProcessor processor = new scribe.Processor<>(new scribe.Iface()
        {
            @Override
            public ResultCode Log(List<LogEntry> messages)
                    throws TException
            {
                for (LogEntry message : messages) {
                    log.info("{}: {}", message.getCategory(),
                             message.getMessage());
                }
                return ResultCode.OK;
            }
        });

        return new ScopedNiftyServer(defaultServerDefBuilder(processor));
    }
}
