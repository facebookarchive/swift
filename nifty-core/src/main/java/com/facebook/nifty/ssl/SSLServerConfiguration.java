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

import com.google.common.base.Preconditions;
import org.jboss.netty.handler.ssl.SslContext;
import org.jboss.netty.handler.ssl.SslHandler;

import java.io.File;

public class SSLServerConfiguration {

    public static class Builder {

        public File keyFile;
        public File certFile;
        public Iterable<String> ciphers;
        public SSLImplProvider sslProvider = new JDKSSLImplProvider();
        boolean allowPlaintext;

        public Builder ciphers(Iterable<String> ciphers) {
            this.ciphers = ciphers;
            return this;
        }

        public Builder keyFile(File keyFile) {
            this.keyFile = keyFile;
            return this;
        }

        public Builder certFile(File certFile) {
            this.certFile = certFile;
            return this;
        }

        public Builder sslProvider(SSLImplProvider sslProvider) {
            this.sslProvider = sslProvider;
            return this;
        }

        /**
         * Whether or not to allow plaintext traffic on a secure port.
         */
        public Builder allowPlaintext(boolean allowPlaintext) {
            this.allowPlaintext = allowPlaintext;
            return this;
        }

        public SSLServerConfiguration build() {
            Preconditions.checkNotNull(keyFile);
            Preconditions.checkNotNull(certFile);

            return new SSLServerConfiguration(this);
        }
    }

    public final Iterable<String> ciphers;
    public final File keyFile;
    public final File certFile;
    public final SSLImplProvider sslProvider;
    public final boolean allowPlaintext;

    public SSLServerConfiguration(Builder builder) {
        this.ciphers = builder.ciphers;
        this.keyFile = builder.keyFile;
        this.certFile = builder.certFile;
        this.sslProvider = builder.sslProvider;
        this.allowPlaintext = builder.allowPlaintext;

        sslProvider.initializeProvider();
    }

    public SslHandler createHandler() throws Exception {
        SslContext serverContext =
                SslContext.newServerContext(
                        sslProvider.getSSLProvider(),
                        null,
                        certFile,
                        keyFile,
                        null,
                        ciphers,
                        null,
                        0,
                        0);
        return serverContext.newHandler();
    }
}
