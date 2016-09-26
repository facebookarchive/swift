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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SessionTicketKey;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.io.File;
import java.util.Arrays;

public class OpenSslServerConfiguration extends SslServerConfiguration {

    public enum SSLVersion {
        TLS(SSL.SSL_PROTOCOL_TLS), // Server will accept all TLS versions
        TLS1_2(SSL.SSL_PROTOCOL_TLSV1_2); // Server will accept only TLS1.2.

        private final int id;

        SSLVersion(int id) {
            this.id = id;
        }
        public int getValue() { return id; }
    };

    public enum SSLVerification {
        VERIFY_NONE(SSL.SSL_CVERIFY_NONE), // No client Certificate is required
        VERIFY_OPTIONAL(SSL.SSL_CVERIFY_OPTIONAL), // The client may present a valid Certificate
        VERIFY_REQUIRE(SSL.SSL_CVERIFY_REQUIRE), // The client has to present a valid Certificate
        VERIFY_OPTIONAL_NO_CA(SSL.SSL_CVERIFY_OPTIONAL_NO_CA); // The client's cert does not need to be verifiable.

        private final int id;

        SSLVerification(int id) {
            this.id = id;
        }
        public int getValue() { return id; }
    };

    public static class Builder extends SslServerConfiguration.BuilderBase<Builder> {

        // Note: when adding new fields, make sure to update the initFromConfiguration() method below.
        public SessionTicketKey[] ticketKeys;
        // A string that can be used to separate tickets from different entities.
        public String sessionContext = "thrift";
        public long sessionTimeoutSeconds = 86400;
        public long sessionCacheSize = 0;
        public boolean enableStatefulSessionCache = true;
        public SSLVersion sslVersion = SSLVersion.TLS1_2;
        public Iterable<String> nextProtocols = ImmutableList.of("thrift");
        public File clientCAFile;
        public SSLVerification sslVerification = SSLVerification.VERIFY_OPTIONAL;

        public Builder() {
            this.ciphers = SslDefaults.SERVER_DEFAULTS;
        }

        /**
         * Multiple session tickets can be set to allow for ticket rotation.
         * The first key is the active key used to encrypt tickets for new sessions.
         * Other ticket keys can be used to decrypt tickets and are not active keys.
         */
        public Builder ticketKeys(SessionTicketKey[] ticketKeys) {
            this.ticketKeys = ticketKeys == null ? null : Arrays.copyOf(ticketKeys, ticketKeys.length);
            return this;
        }

        /**
         * Sets the next protocols which will be set to both ALPN as well as NPN.
         */
        public Builder nextProtocols(Iterable<String> nextProtocols) {
            this.nextProtocols = ImmutableList.copyOf(nextProtocols);
            return this;
        }

        /**
         * Can be used to separate the tickets issued from different services
         * generated with the same key.
         */
        public Builder sessionContext(String sessionContext) {
            this.sessionContext = sessionContext;
            return this;
        }

        public Builder sessionTimeoutSeconds(long sessionTimeoutSeconds) {
            this.sessionTimeoutSeconds = sessionTimeoutSeconds;
            return this;
        }

        public Builder sessionCacheSize(long sessionCacheSize) {
            this.sessionCacheSize = sessionCacheSize;
            return this;
        }

        public Builder sslVersion(SSLVersion sslVersion) {
            this.sslVersion = sslVersion;
            return this;
        }

        public Builder enableStatefulSessionCache(boolean enabled) {
            this.enableStatefulSessionCache = enabled;
            return this;
        }

        /**
         * Copies the state of an existing configration into this builder.
         * @param config the SSL configuration.
         * @return this builder.
         */
        @Override
        public Builder initFromConfiguration(SslServerConfiguration config) {
            super.initFromConfiguration(config);
            if (config instanceof OpenSslServerConfiguration) {
                OpenSslServerConfiguration openSslConfig = (OpenSslServerConfiguration) config;
                ticketKeys(openSslConfig.ticketKeys);
                sessionContext(new String(openSslConfig.sessionContext));
                sessionTimeoutSeconds(openSslConfig.sessionTimeoutSeconds);
                sessionCacheSize(openSslConfig.sessionCacheSize);
                sslVersion(openSslConfig.sslVersion);
                nextProtocols(openSslConfig.nextProtocols);
                clientCAFile(openSslConfig.clientCAFile);
                sslVerification(openSslConfig.sslVerification);
            } else {
                throw new IllegalArgumentException("Provided configuration is not an OpenSslServerConfiguration");
            }
            return this;
        }

        public Builder clientCAFile(File clientCAFile) {
            this.clientCAFile = clientCAFile;
            return this;
        }

        public Builder sslVerification(SSLVerification sslVerification) {
            this.sslVerification = sslVerification;
            return this;
        }

        @Override
        protected SslServerConfiguration createServerConfiguration() {
            OpenSslServerConfiguration sslServerConfiguration = new OpenSslServerConfiguration(this);
            sslServerConfiguration.initializeServerContext();
            return sslServerConfiguration;
        }
    }

    public final SessionTicketKey[] ticketKeys;
    // A string that can be used to separate tickets from different entities.
    public final byte[] sessionContext;
    public final long sessionTimeoutSeconds;
    public final long sessionCacheSize;
    public final boolean enableStatefulSessionCache;
    public final SSLVersion sslVersion;
    public final Iterable<String> nextProtocols;
    public final File clientCAFile;
    public final SSLVerification sslVerification;

    private OpenSslServerConfiguration(Builder builder) {
        super(builder);
        this.ticketKeys = builder.ticketKeys;
        this.sessionContext = builder.sessionContext.getBytes();
        this.sessionTimeoutSeconds = builder.sessionTimeoutSeconds;
        this.sessionCacheSize = builder.sessionCacheSize;
        this.enableStatefulSessionCache = builder.enableStatefulSessionCache;
        this.sslVersion = builder.sslVersion;
        this.nextProtocols = builder.nextProtocols;
        this.clientCAFile = builder.clientCAFile;
        this.sslVerification = builder.sslVerification;
    }

    public static OpenSslServerConfiguration.Builder newBuilder() {
        return new OpenSslServerConfiguration.Builder();
    }

    @Override
    protected SslHandlerFactory createSslHandlerFactory() {
        NettyTcNativeLoader.ensureAvailable();
        try {
            NiftyOpenSslServerContext serverContext = new NiftyOpenSslServerContext(this);
            if (this.ticketKeys != null) {
                serverContext.setTicketKeys(this.ticketKeys);
            }
            serverContext.setSessionIdContext(this.sessionContext);
            serverContext.setSessionCacheTimeout(this.sessionTimeoutSeconds);
            return serverContext;
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public SslSession getSession(SSLEngine engine) throws SSLException {
        return OpenSslSessionHelper.getSession(engine);
    }
}
