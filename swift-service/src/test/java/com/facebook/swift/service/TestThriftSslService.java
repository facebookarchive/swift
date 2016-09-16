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
package com.facebook.swift.service;

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.nifty.client.NettyClientConfig;
import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.core.NiftyTimer;
import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.nifty.processor.NiftyProcessorAdapters;
import com.facebook.nifty.ssl.OpenSslServerConfiguration;
import com.facebook.nifty.ssl.SslClientConfiguration;
import com.facebook.nifty.ssl.SslServerConfiguration;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.scribe.LogEntry;
import com.facebook.swift.service.scribe.ResultCode;
import com.facebook.swift.service.scribe.scribe;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.jboss.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.net.HostAndPort.fromParts;
import static org.testng.Assert.*;

/**
 * Demonstrates creating a Thrift service using Swift with ssl
 */
public class TestThriftSslService
{
    @Test
    public void testSwiftService()
            throws Exception
    {
        SwiftScribe scribeService = new SwiftScribe();
        NiftyProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), ImmutableList.<ThriftEventHandler>of(), scribeService);

        List<LogEntry> messages = testProcessor(processor, false);
        assertEquals(scribeService.getMessages(), newArrayList(concat(toSwiftLogEntry(messages), toSwiftLogEntry(messages))));
    }

    @Test
    public void testThriftService()
            throws Exception
    {
        ThriftScribeService scribeService = new ThriftScribeService();
        TProcessor processor = new scribe.Processor<>(scribeService);

        List<LogEntry> messages = testProcessor(processor);
        assertEquals(scribeService.getMessages(), newArrayList(concat(messages, messages)));
    }

    @Test
    public void testSwiftServicePlaintext()
            throws Exception
    {
        SwiftScribe scribeService = new SwiftScribe();
        NiftyProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), ImmutableList.<ThriftEventHandler>of(), scribeService);

        List<LogEntry> messages = testProcessor(processor, true);
        assertEquals(scribeService.getMessages(), newArrayList(concat(toSwiftLogEntry(messages), toSwiftLogEntry(messages))));
    }

    @Test
    public void testThriftServicePlaintext()
            throws Exception
    {
        ThriftScribeService scribeService = new ThriftScribeService();
        TProcessor processor = new scribe.Processor<>(scribeService);

        List<LogEntry> messages = testProcessor(NiftyProcessorAdapters.processorFromTProcessor(processor), true);
        assertEquals(scribeService.getMessages(), newArrayList(concat(messages, messages)));
    }

    private List<LogEntry> testProcessor(TProcessor processor) throws Exception
    {
        return testProcessor(NiftyProcessorAdapters.processorFromTProcessor(processor), false);
    }

    private List<LogEntry> testProcessor(NiftyProcessor processor, boolean plaintext)
            throws Exception
    {
        ImmutableList<LogEntry> messages = ImmutableList.of(
                new LogEntry("hello", "world"),
                new LogEntry("bye", "world")
        );

        SslServerConfiguration sslConfiguration = OpenSslServerConfiguration.newBuilder()
                .certFile(new File(getClass().getResource("/rsa.crt").getFile()))
                .keyFile(new File(getClass().getResource("/rsa.key").getFile()))
                .allowPlaintext(plaintext)
                .build();
        try (ThriftServer server = new ThriftServer(
                processor,
                new ThriftServerConfig(),
                new NiftyTimer("timer"),
                ThriftServer.DEFAULT_FRAME_CODEC_FACTORIES,
                ThriftServer.DEFAULT_PROTOCOL_FACTORIES,
                ThriftServer.DEFAULT_WORKER_EXECUTORS,
                ThriftServer.DEFAULT_SECURITY_FACTORY,
                new ThriftServer.SslServerConfigurationHolder(sslConfiguration),
                ThriftServer.DEFAULT_TRANSPORT_ATTACH_OBSERVER).start()) {
            assertEquals(logThrift(server.getPort(), messages), ResultCode.OK);
            assertEquals(logSwift(server.getPort(), toSwiftLogEntry(messages)), com.facebook.swift.service.ResultCode.OK);
        }

        return messages;
    }

    private ResultCode logThrift(int port, List<LogEntry> messages)
            throws TException, IOException, NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, InsecureTrustManagerFactory.INSTANCE.getTrustManagers(), null);
        Socket sslSocket = ctx.getSocketFactory().createSocket("localhost", port);
        TSocket socket = new TSocket(sslSocket);
        try {
            TBinaryProtocol tp = new TBinaryProtocol(new TFramedTransport(socket));
            return new scribe.Client(tp).Log(messages);
        }
        finally {
            socket.close();
        }
    }

    private com.facebook.swift.service.ResultCode logSwift(int port, List<com.facebook.swift.service.LogEntry> entries)
            throws Exception
    {
        NettyClientConfig nettyClientConfig =
                NettyClientConfig.newBuilder()
                        .setSSLClientConfiguration(
                                new SslClientConfiguration.Builder()
                                        .caFile(new File(getClass().getResource("/rsa.crt").getFile()))
                                        .build()).build();
        NiftyClient niftyClient = new NiftyClient(nettyClientConfig);
        try (
                ThriftClientManager clientManager =
                        new ThriftClientManager(new ThriftCodecManager(), niftyClient, ImmutableSet.of());
                Scribe scribe = clientManager.createClient(
                        new FramedClientConnector(fromParts("localhost", port)),
                        Scribe.class).get()
        ) {
            return scribe.log(entries);
        }
    }

    private List<com.facebook.swift.service.LogEntry> toSwiftLogEntry(List<LogEntry> messages)
    {
        return Lists.transform(messages, new Function<LogEntry, com.facebook.swift.service.LogEntry>()
        {
            @Override
            public com.facebook.swift.service.LogEntry apply(@Nullable LogEntry input)
            {
                return new com.facebook.swift.service.LogEntry(input.category, input.message);
            }
        });
    }
}
