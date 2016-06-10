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

import org.jboss.netty.handler.ssl.SslContext;
import org.jboss.netty.handler.ssl.SslHandler;

import java.io.File;

public class SSLClientConfiguration {

    public static class Builder {
        Iterable<String> ciphers;
        File caFile;
        long sessionCacheSize = 10000;
        long sessionTimeout = 86400;

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

        public Builder sessionTimeout(long sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public SSLClientConfiguration build() {
            return new SSLClientConfiguration(this);
        }
    }

    public final Iterable<String> ciphers;
    public final File caFile;
    public final long sessionCacheSize;
    public final long sessionTimeout;

    public SSLClientConfiguration(Builder builder) {
        this.ciphers = builder.ciphers;
        this.caFile = builder.caFile;
        this.sessionCacheSize = builder.sessionCacheSize;
        this.sessionTimeout = builder.sessionTimeout;
    }

    public SslHandler createHandler() throws Exception {
        SslContext clientContext =
                SslContext.newClientContext(
                        null,
                        null,
                        caFile,
                        null,
                        ciphers,
                        null,
                        sessionCacheSize,
                        sessionTimeout);
        return clientContext.newHandler();
    }
}
