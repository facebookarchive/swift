/*
 * Copyright (C) 2012-2016 Facebook, Inc.
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
package com.facebook.nifty.ssl;

import com.google.common.collect.ImmutableList;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLContext;
import org.apache.tomcat.jni.SessionTicketKey;
import org.jboss.netty.handler.ssl.OpenSslEngine;
import org.jboss.netty.handler.ssl.SslBufferPool;
import org.jboss.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * A lot of this code is taken from Netty's OpenSslServerContext.
 * https://github.com/netty/netty/blob/3.10/src/main/java/org/jboss/netty/handler/ssl/OpenSslServerContext.java
 * NiftyOpenSslServerContext allows us to control this instantiation of SSLContext with custom options
 * missing in netty. We should be able to get rid of this when we move the dependency
 * to the latest version of netty.
 */
public final class NiftyOpenSslServerContext implements SslHandlerFactory {

    private static final String IGNORABLE_ERROR_PREFIX = "error:00000000:";
    private static final int DEFAULT_CERT_DEPTH = 3;

    private final long aprPool;

    private final List<String> ciphers;
    private final long sessionCacheSize;
    private final long sessionTimeout;
    private final List<String> nextProtocols;

    private final OpenSslServerConfiguration sslServerConfiguration;

    private final SslBufferPool bufferPool = newBufferPool();

    /**
     * The OpenSSL SSL_CTX object
     */
    private final long ctx;

    public NiftyOpenSslServerContext(OpenSslServerConfiguration sslServerConfiguration) throws Exception {
        this.sslServerConfiguration = sslServerConfiguration;
        int sslVersion = this.sslServerConfiguration.sslVersion.getValue();
        File certChainFile = sslServerConfiguration.certFile;
        File keyFile = sslServerConfiguration.keyFile;
        File clientCAFile = sslServerConfiguration.clientCAFile;
        OpenSslServerConfiguration.SSLVerification sslVerification = sslServerConfiguration.sslVerification;

        if (certChainFile == null) {
            throw new NullPointerException("certChainFile");
        }
        if (!certChainFile.isFile()) {
            throw new IllegalArgumentException("certChainFile is not a file: " + certChainFile);
        }
        if (keyFile == null) {
            throw new NullPointerException("keyPath");
        }
        if (!keyFile.isFile()) {
            throw new IllegalArgumentException("keyPath is not a file: " + keyFile);
        }
        if (clientCAFile != null && !clientCAFile.isFile()) {
            throw new IllegalArgumentException("clientCAFile is not a file " + clientCAFile);
        }
        if (sslServerConfiguration.ciphers == null) {
            ciphers = SslDefaults.SERVER_DEFAULTS;
        } else {
            ciphers = ImmutableList.copyOf(sslServerConfiguration.ciphers);
        }

        String keyPassword = sslServerConfiguration.keyPassword == null ? "" : sslServerConfiguration.keyPassword;
        if (sslServerConfiguration.nextProtocols == null) {
            nextProtocols = Collections.emptyList();
        } else {
            nextProtocols = ImmutableList.copyOf(sslServerConfiguration.nextProtocols);
        }

        // Allocate a new APR pool.
        aprPool = Pool.create(0);

        // Create a new SSL_CTX and configure it.
        boolean success = false;
        try {
            synchronized (NiftyOpenSslServerContext.class) {
                try {
                    ctx = SSLContext.make(aprPool, sslVersion, SSL.SSL_MODE_SERVER);
                }
                catch (Exception e) {
                    throw new SSLException("failed to create an SSL_CTX", e);
                }

                SSLContext.setOptions(ctx, SSL.SSL_OP_ALL);
                SSLContext.setOptions(ctx, SSL.SSL_OP_NO_SSLv2);
                SSLContext.setOptions(ctx, SSL.SSL_OP_NO_SSLv3);
                SSLContext.setOptions(ctx, SSL.SSL_OP_CIPHER_SERVER_PREFERENCE);
                SSLContext.setOptions(ctx, SSL.SSL_OP_SINGLE_ECDH_USE);
                SSLContext.setOptions(ctx, SSL.SSL_OP_SINGLE_DH_USE);
                SSLContext.setOptions(ctx, SSL.SSL_OP_NO_SESSION_RESUMPTION_ON_RENEGOTIATION);
                SSLContext.setOptions(ctx, SSL.SSL_OP_NO_COMPRESSION);

                if (!this.sslServerConfiguration.enableStatefulSessionCache) {
                    SSLContext.setSessionCacheMode(ctx, OpenSSLConstants.SSL_SESS_CACHE_NO_INTERNAL);
                }

                // We need to enable SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER as the memory address may change between
                // calling OpenSSLEngine.wrap(...).
                // See https://github.com/netty/netty-tcnative/issues/100
                SSLContext.setMode(ctx, SSLContext.getMode(ctx) | SSL.SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER);

                /* List the ciphers that the client is permitted to negotiate. */
                try {
                    // Convert the cipher list into a colon-separated string.
                    StringBuilder cipherBuf = new StringBuilder();
                    for (String c : ciphers) {
                        cipherBuf.append(c);
                        cipherBuf.append(':');
                    }
                    cipherBuf.setLength(cipherBuf.length() - 1);

                    SSLContext.setCipherSuite(ctx, cipherBuf.toString());
                }
                catch (SSLException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new SSLException("failed to set cipher suite: " + ciphers, e);
                }

                /* Load the certificate file and private key. */
                try {
                    if (!SSLContext.setCertificate(
                            ctx, certChainFile.getPath(), keyFile.getPath(), keyPassword, SSL.SSL_AIDX_RSA)) {
                        throw new SSLException("failed to set certificate: " +
                                certChainFile + " and " + keyFile + " (" + SSL.getLastError() + ')');
                    }
                }
                catch (SSLException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new SSLException("failed to set certificate: " + certChainFile + " and " + keyFile, e);
                }

                /* Load the certificate chain. We must skip the first cert since it was loaded above. */
                if (!SSLContext.setCertificateChainFile(ctx, certChainFile.getPath(), true)) {
                    String error = SSL.getLastError();
                    if (!error.startsWith(IGNORABLE_ERROR_PREFIX)) {
                        throw new SSLException(
                                "failed to set certificate chain: " + certChainFile + " (" + SSL.getLastError() + ')');
                    }
                }

                if (clientCAFile != null &&
                    !SSLContext.setCACertificate(ctx, clientCAFile.getPath(), null)) {
                    String error = SSL.getLastError();
                    if (!error.startsWith(IGNORABLE_ERROR_PREFIX)) {
                        throw new SSLException(
                                "failed to set ca cert: " + clientCAFile + " (" + SSL.getLastError() + ')');
                    }
                }

                SSLContext.setVerify(ctx, sslVerification.getValue(), DEFAULT_CERT_DEPTH);

                /* Set next protocols for next protocol negotiation extension, if specified */
                if (!nextProtocols.isEmpty()) {
                    // Convert the protocol list into a comma-separated string.
                    StringBuilder nextProtocolBuf = new StringBuilder();
                    for (String p : nextProtocols) {
                        nextProtocolBuf.append(p);
                        nextProtocolBuf.append(',');
                    }
                    nextProtocolBuf.setLength(nextProtocolBuf.length() - 1);

                    SSLContext.setNextProtos(ctx, nextProtocolBuf.toString());
                }
                if (nextProtocols != null && !nextProtocols.isEmpty()) {
                    String[] alpnArray = nextProtocols.toArray(new String[0]);
                    SSLContext.setAlpnProtos(ctx, alpnArray, SSL.SSL_SELECTOR_FAILURE_CHOOSE_MY_LAST_PROTOCOL);
                }

                /* Set session cache size, if specified */
                if (sslServerConfiguration.sessionCacheSize > 0) {
                    sessionCacheSize = sslServerConfiguration.sessionCacheSize;
                    SSLContext.setSessionCacheSize(ctx, sessionCacheSize);
                } else {
                    // Get the default session cache size using SSLContext.setSessionCacheSize()
                    sessionCacheSize = SSLContext.setSessionCacheSize(ctx, 20480);
                    // Revert the session cache size to the default value.
                    SSLContext.setSessionCacheSize(ctx, sessionCacheSize);
                }

                /* Set session timeout, if specified */
                if (sslServerConfiguration.sessionTimeoutSeconds > 0) {
                    sessionTimeout = sslServerConfiguration.sessionTimeoutSeconds;
                    SSLContext.setSessionCacheTimeout(ctx, sslServerConfiguration.sessionTimeoutSeconds);
                } else {
                    // Get the default session timeout using SSLContext.setSessionCacheTimeout()
                    sessionTimeout = SSLContext.setSessionCacheTimeout(ctx, 300);
                    // Revert the session timeout to the default value.
                    SSLContext.setSessionCacheTimeout(ctx, sessionTimeout);
                }
            }
            success = true;
        }
        finally {
            if (!success) {
                destroyPools();
            }
        }
    }

    private static SslBufferPool newBufferPool() {
        return new SslBufferPool(true, true);
    }

    public List<String> cipherSuites() {
        return ImmutableList.copyOf(ciphers);
    }

    public long sessionCacheSize() {
        return sessionCacheSize;
    }

    public long sessionTimeout() {
        return sessionTimeout;
    }

    public List<String> nextProtocols() {
        return nextProtocols;
    }

    /**
     * Returns the {@code SSL_CTX} object of this context.
     */
    public long context() {
        return ctx;
    }

    /**
     * Returns a new server-side {@link SSLEngine} with the current configuration.
     */
    public SSLEngine newEngine() {
        if (nextProtocols.isEmpty()) {
            return new OpenSslEngine(ctx, bufferPool, null);
        } else {
            return new OpenSslEngine(
                    ctx, bufferPool, nextProtocols.get(nextProtocols.size() - 1));
        }
    }

    /**
     * Sets the SSL session ticket keys of this context.
     */
    public void setTicketKeys(SessionTicketKey[] keys) {
        if (keys == null) {
            throw new NullPointerException("keys");
        }
        SSLContext.setSessionTicketKeys(ctx, keys);
    }

    public void setSessionIdContext(byte[] sessionIdContext) {
        SSLContext.setSessionIdContext(ctx, sessionIdContext);
    }

    public void setSessionCacheTimeout(long sessionTimeoutSeconds) {
        SSLContext.setSessionCacheTimeout(ctx, sessionTimeoutSeconds);
    }

    @Override
    public SslHandler newHandler() {
        SslHandler handler = new BetterSslHandler(newEngine(), bufferPool, sslServerConfiguration);
        handler.setCloseOnSSLException(true);
        return handler;
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        super.finalize();
        synchronized (NiftyOpenSslServerContext.class) {
            if (ctx != 0) {
                SSLContext.free(ctx);
            }
        }

        destroyPools();
    }

    private void destroyPools() {
        if (aprPool != 0) {
            Pool.destroy(aprPool);
        }
    }
}
