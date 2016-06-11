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
import com.facebook.nifty.client.NettyClientConfig;
import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.core.*;
import com.facebook.nifty.ssl.OpensslImplProvider;
import com.facebook.nifty.ssl.SSLClientConfiguration;
import com.facebook.nifty.ssl.SSLServerConfiguration;
import com.facebook.nifty.test.LogEntry;
import com.facebook.nifty.test.ResultCode;
import com.facebook.nifty.test.scribe;
import io.airlift.log.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

public class TestNiftySSLServer
{
    private static final Logger log = Logger.get(TestNiftySSLServer.class);
    private NettyServerTransport server;
    private int port;

    @BeforeMethod(alwaysRun = true)
    public void setup()
    {
        server = null;
    }

    @AfterMethod(alwaysRun = true)
    public void teardown()
            throws InterruptedException
    {
        if (server != null) {
            server.stop();
        }
    }
    private void startServer()
    {
        startServer(getThriftServerDefBuilder(false));
    }

    private void startServer(final ThriftServerDefBuilder thriftServerDefBuilder)
    {
        server = new NettyServerTransport(thriftServerDefBuilder.build(),
                                          NettyServerConfig.newBuilder().build(),
                                          new DefaultChannelGroup());
        server.start();
        port = ((InetSocketAddress)server.getServerChannel().getLocalAddress()).getPort();
    }

    private ThriftServerDefBuilder getThriftServerDefBuilder(boolean allowPlaintext)
    {
        return new ThriftServerDefBuilder()
                .listen(8080)
                .withSSLConfiguration(
                        new SSLServerConfiguration.Builder()
                                .certFile(new File(Plain.class.getResource("/rsa.crt").getFile()))
                                .keyFile(new File(Plain.class.getResource("/rsa.key").getFile()))
                                .sslProvider(new OpensslImplProvider())
                                .allowPlaintext(allowPlaintext)
                                .build())
                .withProcessor(new scribe.Processor<>(new scribe.Iface() {
                    @Override
                    public ResultCode Log(List<LogEntry> messages)
                            throws TException {
                        RequestContext context = RequestContexts.getCurrentContext();

                        for (LogEntry message : messages) {
                            log.info("[Client: %s] %s: %s",
                                    context.getConnectionContext().getRemoteAddress(),
                                    message.getCategory(),
                                    message.getMessage());
                        }
                        return ResultCode.OK;
                    }
                }));
    }

    private scribe.Client makeNiftyClient()
            throws TTransportException, InterruptedException
    {
        NettyClientConfig config =
                NettyClientConfig.newBuilder()
                        .setSSLClientConfiguration(
                                new SSLClientConfiguration.Builder()
                                        .caFile(new File(Plain.class.getResource("/rsa.crt").getFile()))
                                        .build()).build();
        InetSocketAddress address = new InetSocketAddress("localhost", port);
        TTransport transport = new NiftyClient(config)
                .connectSync(scribe.Client.class, new FramedClientConnector(address));
        TProtocol protocol = new TBinaryProtocol(transport);
        return new scribe.Client(protocol);
    }

    private scribe.Client makeNiftyPlaintextClient()
            throws TTransportException, InterruptedException
    {
        NettyClientConfig config =
                NettyClientConfig.newBuilder().build();
        InetSocketAddress address = new InetSocketAddress("localhost", port);
        TTransport transport = new NiftyClient(config)
                .connectSync(scribe.Client.class, new FramedClientConnector(address));
        TProtocol protocol = new TBinaryProtocol(transport);
        return new scribe.Client(protocol);
    }

    @Test
    public void testSSL() throws InterruptedException, TException
    {
        startServer();
        scribe.Client client1 = makeNiftyClient();
        Assert.assertEquals(client1.Log(Arrays.asList(new LogEntry("client1", "aaa"))), ResultCode.OK);
        Assert.assertEquals(client1.Log(Arrays.asList(new LogEntry("client1", "bbb"))), ResultCode.OK);
        scribe.Client client2 = makeNiftyClient();
        Assert.assertEquals(client2.Log(Arrays.asList(new LogEntry("client2", "ccc"))), ResultCode.OK);
    }

    @Test
    public void testSSLWithPlaintextAllowedServer() throws InterruptedException, TException
    {
        startServer(getThriftServerDefBuilder(true));
        scribe.Client client1 = makeNiftyClient();
        Assert.assertEquals(client1.Log(Arrays.asList(new LogEntry("client1", "aaa"))), ResultCode.OK);
        Assert.assertEquals(client1.Log(Arrays.asList(new LogEntry("client1", "bbb"))), ResultCode.OK);
        scribe.Client client2 = makeNiftyClient();
        Assert.assertEquals(client2.Log(Arrays.asList(new LogEntry("client2", "ccc"))), ResultCode.OK);
    }

    @Test(expectedExceptions = TTransportException.class)
    public void testUnencryptedClient() throws InterruptedException, TException
    {
        startServer();
        scribe.Client client = makeNiftyPlaintextClient();
        client.Log(Arrays.asList(new LogEntry("client2", "aaa")));
        client.Log(Arrays.asList(new LogEntry("client2", "bbb")));
        client.Log(Arrays.asList(new LogEntry("client2", "ccc")));
    }

    @Test
    public void testUnencryptedClientWithAllowPlaintextServer() throws InterruptedException, TException
    {
        startServer(getThriftServerDefBuilder(true));
        scribe.Client client = makeNiftyPlaintextClient();
        client.Log(Arrays.asList(new LogEntry("client2", "aaa")));
        client.Log(Arrays.asList(new LogEntry("client2", "bbb")));
        client.Log(Arrays.asList(new LogEntry("client2", "ccc")));
    }
}
