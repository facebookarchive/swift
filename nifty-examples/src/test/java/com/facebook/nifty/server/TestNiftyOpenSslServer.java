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
import com.facebook.nifty.client.TNiftyClientChannelTransport;
import com.facebook.nifty.core.*;
import com.facebook.nifty.ssl.*;
import com.facebook.nifty.test.LogEntry;
import com.facebook.nifty.test.ResultCode;
import com.facebook.nifty.test.scribe;
import com.google.common.base.Throwables;
import io.airlift.log.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.tomcat.jni.SessionTicketKey;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.ssl.SslHandler;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.*;
import java.util.Arrays;
import java.util.List;

public class TestNiftyOpenSslServer
{
    private static final Logger log = Logger.get(TestNiftyOpenSslServer.class);
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
        startServer(getThriftServerDefBuilder(createSSLServerConfiguration(false, null), null));
    }

    private void startServer(final ThriftServerDefBuilder thriftServerDefBuilder)
    {
        server = new NettyServerTransport(thriftServerDefBuilder.build(),
                                          NettyServerConfig.newBuilder().build(),
                                          new DefaultChannelGroup());
        server.start();
        port = ((InetSocketAddress)server.getServerChannel().getLocalAddress()).getPort();
    }

    SslServerConfiguration createSSLServerConfiguration(boolean allowPlaintext, SessionTicketKey[] ticketKeys) {
        return OpenSslServerConfiguration.newBuilder()
                .certFile(new File(Plain.class.getResource("/rsa.crt").getFile()))
                .keyFile(new File(Plain.class.getResource("/rsa.key").getFile()))
                .allowPlaintext(allowPlaintext)
                .ticketKeys(ticketKeys)
                .build();
    }

    private ThriftServerDefBuilder getThriftServerDefBuilder(
            SslServerConfiguration sslServerConfiguration,
            TransportAttachObserver configUpdater)
    {
        return new ThriftServerDefBuilder()
                .listen(0)
                .withSSLConfiguration(sslServerConfiguration)
                .withTransportAttachObserver(configUpdater)
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

    private static SslClientConfiguration getClientSSLConfiguration() {
        return new SslClientConfiguration.Builder()
                .caFile(new File(Plain.class.getResource("/rsa.crt").getFile()))
                .sessionCacheSize(10000)
                .sessionTimeoutSeconds(10000)
                .build();
    }

    private scribe.Client makeNiftyClient(SslClientConfiguration clientSSLConfiguration)
            throws TTransportException, InterruptedException
    {
        NettyClientConfig config =
                NettyClientConfig.newBuilder()
                        .setSSLClientConfiguration(clientSSLConfiguration).build();
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
        scribe.Client client1 = makeNiftyClient(getClientSSLConfiguration());
        Assert.assertEquals(client1.Log(Arrays.asList(new LogEntry("client1", "aaa"))), ResultCode.OK);
        Assert.assertEquals(client1.Log(Arrays.asList(new LogEntry("client1", "bbb"))), ResultCode.OK);
        scribe.Client client2 = makeNiftyClient(getClientSSLConfiguration());
        Assert.assertEquals(client2.Log(Arrays.asList(new LogEntry("client2", "ccc"))), ResultCode.OK);
    }

    @Test
    public void testSSLWithPlaintextAllowedServer() throws InterruptedException, TException
    {
        startServer(getThriftServerDefBuilder(createSSLServerConfiguration(true, null), null));
        scribe.Client client1 = makeNiftyClient(getClientSSLConfiguration());
        Assert.assertEquals(client1.Log(Arrays.asList(new LogEntry("client1", "aaa"))), ResultCode.OK);
        Assert.assertEquals(client1.Log(Arrays.asList(new LogEntry("client1", "bbb"))), ResultCode.OK);
        scribe.Client client2 = makeNiftyClient(getClientSSLConfiguration());
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
        startServer(getThriftServerDefBuilder(createSSLServerConfiguration(true, null), null));
        scribe.Client client = makeNiftyPlaintextClient();
        client.Log(Arrays.asList(new LogEntry("client2", "aaa")));
        client.Log(Arrays.asList(new LogEntry("client2", "bbb")));
        client.Log(Arrays.asList(new LogEntry("client2", "ccc")));
    }

    private void startClientWithCerts() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            InputStream keyInput = new FileInputStream(Plain.class.getResource("/rsa.p12").getFile());
            keyStore.load(keyInput, "12345".toCharArray());
            keyInput.close();

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "12345".toCharArray());

            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(keyStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagerFactory.getKeyManagers(), factory.getTrustManagers(), null);

            Socket sock = new Socket();
            sock.connect(new InetSocketAddress("localhost", port));
            SSLSocket sslSocket = (SSLSocket) context.getSocketFactory().createSocket(sock, "localhost", port, true);
            sslSocket.startHandshake();
            SSLSession session = sslSocket.getSession();
            Assert.assertTrue(session.isValid());
            sslSocket.close();
        } catch (Throwable t) {
            throw Throwables.propagate(t);
        }
    }

    private SslSession[] addAuthentication(ThriftServerDefBuilder builder, SslServerConfiguration configuration) {
        final SslSession[] sslSession = new SslSession[1];
        builder.withSecurityFactory(new NiftySecurityFactory() {
            @Override
            public NiftySecurityHandlers getSecurityHandlers(ThriftServerDef def, NettyServerConfig serverConfig) {
                return new NiftySecurityHandlers() {
                    @Override
                    public ChannelHandler getAuthenticationHandler() {
                        return new SimpleChannelHandler() {
                            @Override
                            public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
                                super.channelOpen(ctx, e);
                                SslHandler handler = (SslHandler) ctx.getPipeline().get("ssl");
                                handler.handshake().addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture future) throws Exception {
                                        synchronized (TestNiftyOpenSslServer.this) {
                                            sslSession[0] = configuration.getSession(handler.getEngine());
                                            TestNiftyOpenSslServer.this.notify();
                                        }
                                    }
                                });
                                ctx.getPipeline().remove(this);
                            }
                        };
                    }

                    @Override
                    public ChannelHandler getEncryptionHandler() {
                        return NiftyNoOpSecurityFactory.noOpHandler;
                    }
                };
            }
        });
        return sslSession;
    }

    @Test
    public void testDefaultServerWithClientCert() throws InterruptedException {
        SslServerConfiguration serverConfig = OpenSslServerConfiguration.newBuilder()
                .certFile(new File(Plain.class.getResource("/rsa.crt").getFile()))
                .keyFile(new File(Plain.class.getResource("/rsa.key").getFile()))
                .allowPlaintext(false)
                .clientCAFile(new File(Plain.class.getResource("/rsa.crt").getFile()))
                .build();
        ThriftServerDefBuilder builder = getThriftServerDefBuilder(serverConfig, null);
        SslSession[] session = addAuthentication(builder, serverConfig);
        startServer(builder);
        startClientWithCerts();
        synchronized (this) {
            if (session[0] == null) {
                wait(100);
            }
        }
        Assert.assertEquals(session[0].peerCert.getSubjectDN().toString(), "CN=RSA, OU=RSA, O=RSA, L=Default City, C=XX");
    }

    @Test
    public void testClientAuthenticatingServer() throws InterruptedException {
        SslServerConfiguration serverConfig = OpenSslServerConfiguration.newBuilder()
                .certFile(new File(Plain.class.getResource("/rsa.crt").getFile()))
                .keyFile(new File(Plain.class.getResource("/rsa.key").getFile()))
                .allowPlaintext(false)
                .sslVerification(OpenSslServerConfiguration.SSLVerification.VERIFY_REQUIRE)
                .clientCAFile(new File(Plain.class.getResource("/rsa.crt").getFile()))
                .build();

        ThriftServerDefBuilder builder = getThriftServerDefBuilder(serverConfig, null);
        SslSession[] session = addAuthentication(builder, serverConfig);
        startServer(builder);
        startClientWithCerts();
        // Waits for max of 100ms for the server thread to process the cert
        synchronized (this) {
            if (session[0] == null) {
                wait(100);
            }
        }
        Assert.assertEquals(session[0].peerCert.getSubjectDN().toString(), "CN=RSA, OU=RSA, O=RSA, L=Default City, C=XX");
    }

    @Test(expectedExceptions = TTransportException.class)
    public void testClientWithoutCerts() throws InterruptedException, TException {
        SslServerConfiguration serverConfig = OpenSslServerConfiguration.newBuilder()
                .certFile(new File(Plain.class.getResource("/rsa.crt").getFile()))
                .keyFile(new File(Plain.class.getResource("/rsa.key").getFile()))
                .allowPlaintext(false)
                .sslVerification(OpenSslServerConfiguration.SSLVerification.VERIFY_REQUIRE)
                .clientCAFile(new File(Plain.class.getResource("/rsa.crt").getFile()))
                .build();

        startServer(getThriftServerDefBuilder(serverConfig, null));
        SslClientConfiguration sslClientConfiguration = getClientSSLConfiguration();

        scribe.Client client1 = makeNiftyClient(sslClientConfiguration);
        client1.Log(Arrays.asList(new LogEntry("client1", "aaa")));
    }

    @Test
    public void testSSLSessionResumption() throws Exception {
        // Ticket resumes are not supported by nifty client, so we test stateful session resumption
        // only.
        SessionTicketKey[] keys = { createSessionTicketKey() };
        SslServerConfiguration sslServerConfiguration = createSSLServerConfiguration(true, keys);
        startServer(getThriftServerDefBuilder(sslServerConfiguration, null));

        SslClientConfiguration sslClientConfiguration = getClientSSLConfiguration();

        scribe.Client client1 = makeNiftyClient(sslClientConfiguration);
        client1.Log(Arrays.asList(new LogEntry("client1", "aaa")));
        Assert.assertFalse(isSessionResumed(getSSLSession(client1)));

        scribe.Client client2 = makeNiftyClient(sslClientConfiguration);
        client2.Log(Arrays.asList(new LogEntry("client2", "aaa")));
        Assert.assertTrue(isSessionResumed(getSSLSession(client2)));

        client2.Log(Arrays.asList(new LogEntry("client2", "bbb")));
        Assert.assertTrue(isSessionResumed(getSSLSession(client2)));

        SessionTicketKey[] keys2 = { createSessionTicketKey() };
        SslServerConfiguration sslServerConfiguration2 = createSSLServerConfiguration(true, keys2);
        server.updateSSLConfiguration(sslServerConfiguration2);

        scribe.Client client3 = makeNiftyClient(sslClientConfiguration);
        client3.Log(Arrays.asList(new LogEntry("client3", "aaa")));
        Assert.assertFalse(isSessionResumed(getSSLSession(client3)));

        scribe.Client client4 = makeNiftyClient(sslClientConfiguration);
        client4.Log(Arrays.asList(new LogEntry("client4", "aaa")));
        Assert.assertTrue(isSessionResumed(getSSLSession(client4)));
    }

    class TestConfigUpdater implements TransportAttachObserver {

        public NettyServerTransport attachedTransport;

        @Override
        public void attachTransport(NettyServerTransport transport) {
            attachedTransport = transport;
        }

        @Override
        public void detachTransport() {
            attachedTransport = null;
        }

        void updateSSLConfig(SslServerConfiguration newConfig) {
            attachedTransport.updateSSLConfiguration(newConfig);
        }
    };

    @Test
    public void testAttachTransportToUpdater() throws InterruptedException {
        TestConfigUpdater configUpdater = new TestConfigUpdater();
        SessionTicketKey[] keys = { createSessionTicketKey() };
        SslServerConfiguration sslServerConfiguration = createSSLServerConfiguration(true, keys);
        startServer(getThriftServerDefBuilder(sslServerConfiguration, configUpdater));
        Assert.assertNotNull(configUpdater.attachedTransport);

        SessionTicketKey[] newKeys = { createSessionTicketKey() };
        SslServerConfiguration newConfig = createSSLServerConfiguration(true, newKeys);
        configUpdater.updateSSLConfig(newConfig);

        server.stop();
        server = null;
        Assert.assertNull(configUpdater.attachedTransport);
    }

    private static SessionTicketKey createSessionTicketKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] name = new byte[SessionTicketKey.NAME_SIZE];
        byte[] hmac = new byte[SessionTicketKey.HMAC_KEY_SIZE];
        byte[] aes = new byte[SessionTicketKey.AES_KEY_SIZE];
        secureRandom.nextBytes(name);
        secureRandom.nextBytes(hmac);
        secureRandom.nextBytes(aes);

        return new SessionTicketKey(name, hmac, aes);
    }

    private static SSLSession getSSLSession(scribe.Client client) {
        TNiftyClientChannelTransport clientTransport =
                (TNiftyClientChannelTransport) client.getInputProtocol().getTransport();
        SslHandler sslHandler = (SslHandler) clientTransport.getChannel().getNettyChannel().getPipeline().get("ssl");
        return sslHandler.getEngine().getSession();
    }

    private static boolean isSessionResumed(SSLSession sslSession) throws NoSuchFieldException, IllegalAccessException {
        Field sslResumedField =  sslSession.getClass().getDeclaredField("isSessionResumption");
        sslResumedField.setAccessible(true);
        return sslResumedField.getBoolean(sslSession);
    }
}
