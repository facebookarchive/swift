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
import org.jboss.netty.handler.ssl.SslContext;
import org.jboss.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SslClientConfiguration {

    public static class Builder {
        Iterable<String> ciphers;
        File caFile;
        long sessionCacheSize = 10000;
        long sessionTimeoutSeconds = 86400;
        SslContext clientContext;

        public Builder ciphers(Iterable<String> ciphers) {
            this.ciphers = ciphers;
            return this;
        }

        public Builder caFile(File caFile) {
            this.caFile = caFile;
            return this;
        }

        public Builder sessionCacheSize(long sessionCacheSize) {
            this.sessionCacheSize = sessionCacheSize;
            return this;
        }

        public Builder sessionTimeoutSeconds(long sessionTimeoutSeconds) {
            this.sessionTimeoutSeconds = sessionTimeoutSeconds;
            return this;
        }

        /**
         * Overrides the SslContext with one explicitly provided by the caller. If this is not null, the other
         * builder parameters will be ignored. Currently only used for testing and may be removed in the future,
         * once we have netty support for client-side certs.
         *
         * @param clientContext the client context.
         * @return a reference to this builder.
         */
        public Builder sslContext(SslContext clientContext) {
            this.clientContext = clientContext;
            return this;
        }

        public SslClientConfiguration build() {
            return new SslClientConfiguration(this);
        }
    }

    private SslContext clientContext;

    public SslClientConfiguration(Builder builder) {
        if (builder.clientContext == null) {
            try {
                clientContext =
                    SslContext.newClientContext(
                        null,
                        null,
                        builder.caFile,
                        null,
                        builder.ciphers,
                        null,
                        builder.sessionCacheSize,
                        builder.sessionTimeoutSeconds);
            } catch (SSLException e) {
                Throwables.propagate(e);
            }
        } else {
            clientContext = builder.clientContext;
        }
    }

    public SslHandler createHandler() throws Exception {
        return clientContext.newHandler();
    }

    public SslHandler createHandler(SocketAddress address) throws Exception {
        if (!(address instanceof InetSocketAddress)) {
            return createHandler();
        }
        InetSocketAddress netAddress = (InetSocketAddress) address;
        String host = netAddress.getHostString();
        if (host == null) {
            return createHandler();
        }

        return clientContext.newHandler(host, netAddress.getPort());
    }
}
