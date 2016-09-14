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

import javax.net.ssl.SSLException;
import java.util.Arrays;
import java.util.List;

public class OpenSslServerConfiguration extends SslServerConfiguration {

    public enum SSLVersion {
        TLS, // Server will accept all TLS versions
        TLS1_2, // Server will accept only TLS1.2.
    };

    public static class Builder extends SslServerConfiguration.BuilderBase<Builder> {

        // Note: when adding new fields, make sure to update the initFromConfiguration() method below.
        public SessionTicketKey[] ticketKeys;
        // A string that can be used to separate tickets from different entities.
        public String sessionContext = "thrift";
        public long sessionTimeoutSeconds = 86400;
        public SSLVersion sslVersion = SSLVersion.TLS1_2;
        public Iterable<String> nextProtocols = ImmutableList.of("thrift");

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

        public Builder sslVersion(SSLVersion sslVersion) {
            this.sslVersion = sslVersion;
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
                sslVersion(openSslConfig.sslVersion);
                nextProtocols(openSslConfig.nextProtocols);
            }
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
    public final SSLVersion sslVersion;
    public final Iterable<String> nextProtocols;

    private OpenSslServerConfiguration(Builder builder) {
        super(builder);
        this.ticketKeys = builder.ticketKeys;
        this.sessionContext = builder.sessionContext.getBytes();
        this.sessionTimeoutSeconds = builder.sessionTimeoutSeconds;
        this.sslVersion = builder.sslVersion;
        this.nextProtocols = builder.nextProtocols;
    }

    public static OpenSslServerConfiguration.Builder newBuilder() {
        return new OpenSslServerConfiguration.Builder();
    }

    @Override
    protected SslHandlerFactory createSslHandlerFactory() {
        NettyTcNativeLoader.ensureAvailable();
        try {
            int sslVersionInt = SSL.SSL_PROTOCOL_TLS;
            if (sslVersion == SSLVersion.TLS1_2) {
                sslVersionInt = SSL.SSL_PROTOCOL_TLSV1_2;
            }
            NiftyOpenSslServerContext serverContext = new NiftyOpenSslServerContext(
                    certFile,
                    keyFile,
                    null,
                    ciphers,
                    sslVersionInt,
                    nextProtocols,
                    0,
                    0);
            if (this.ticketKeys != null) {
                serverContext.setTicketKeys(this.ticketKeys);
            }
            serverContext.setSessionIdContext(this.sessionContext);
            serverContext.setSessionCacheTimeout(this.sessionTimeoutSeconds);
            return serverContext;
        }
        catch (SSLException e) {
            throw Throwables.propagate(e);
        }
    }
}
