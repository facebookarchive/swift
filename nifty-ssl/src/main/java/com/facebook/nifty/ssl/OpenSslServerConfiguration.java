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
package com.facebook.nifty.ssl;

import com.google.common.base.Throwables;
import org.apache.tomcat.jni.SessionTicketKey;
import org.jboss.netty.handler.ssl.OpenSslServerContext;
import org.jboss.netty.handler.ssl.SslContext;
import org.jboss.netty.handler.ssl.SslProvider;

import javax.net.ssl.SSLException;

public class OpenSslServerConfiguration extends SslServerConfiguration {

    public static class Builder extends SslServerConfiguration.BuilderBase<Builder> {

        public SessionTicketKey[] ticketKeys;
        // A string that can be used to separate tickets from different entities.
        public String sessionContext = "thrift";
        public long sessionTimeoutSeconds = 86400;

        public Builder() {
            this.ciphers = SslDefaults.SERVER_DEFAULTS;
        }

        /**
         * Multiple session tickets can be set to allow for ticket rotation.
         * The first key is the active key used to encrypt tickets for new sessions.
         * Other ticket keys can be used to decrypt tickets and are not active keys.
         */
        public Builder ticketKeys(SessionTicketKey[] ticketKeys) {
            this.ticketKeys = ticketKeys;
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

    private OpenSslServerConfiguration(Builder builder) {
        super(builder);
        this.ticketKeys = builder.ticketKeys;
        this.sessionContext = builder.sessionContext.getBytes();
        this.sessionTimeoutSeconds = builder.sessionTimeoutSeconds;
    }

    public static OpenSslServerConfiguration.Builder newBuilder() {
        return new OpenSslServerConfiguration.Builder();
    }

    @Override
    protected SslContext createServerContext() {
        NettyTcNativeLoader.ensureAvailable();
        SslContext serverContext = null;
        try {
            serverContext =
                    SslContext.newServerContext(
                            SslProvider.OPENSSL,
                            null,
                            certFile,
                            keyFile,
                            null,
                            ciphers,
                            null,
                            0,
                            0);
        }
        catch (SSLException e) {
            throw Throwables.propagate(e);
        }
        if (serverContext instanceof OpenSslServerContext) {
            OpenSslServerContext opensslCtx = (OpenSslServerContext) serverContext;
            long ctxPtr = opensslCtx.context();
            if (this.ticketKeys != null) {
                org.apache.tomcat.jni.SSLContext.setSessionTicketKeys(ctxPtr, this.ticketKeys);
            }
            org.apache.tomcat.jni.SSLContext.setSessionIdContext(ctxPtr, this.sessionContext);
            org.apache.tomcat.jni.SSLContext.setSessionCacheTimeout(ctxPtr, this.sessionTimeoutSeconds);
        }
        return serverContext;
    }
}
