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
import org.jboss.netty.handler.ssl.SslHandler;

import java.io.File;

public abstract class SslServerConfiguration {

    public abstract static class BuilderBase<T> {

        public File keyFile;
        public File certFile;
        public Iterable<String> ciphers;
        boolean allowPlaintext;

        public T ciphers(Iterable<String> ciphers) {
            this.ciphers = ciphers;
            return (T) this;
        }

        public T keyFile(File keyFile) {
            this.keyFile = keyFile;
            return (T) this;
        }

        public T certFile(File certFile) {
            this.certFile = certFile;
            return (T) this;
        }

        /**
         * Whether or not to allow plaintext traffic on a secure port.
         */
        public T allowPlaintext(boolean allowPlaintext) {
            this.allowPlaintext = allowPlaintext;
            return (T) this;
        }

        protected abstract SslServerConfiguration createServerConfiguration();

        /**
         * Builds a server configuration
         *
         * @throws RuntimeException if parameters are not valid.
         */
        public SslServerConfiguration build() {
            Preconditions.checkNotNull(keyFile);
            Preconditions.checkNotNull(certFile);
            return createServerConfiguration();
        }
    }

    public final Iterable<String> ciphers;
    public final File keyFile;
    public final File certFile;
    public final boolean allowPlaintext;

    private SslHandlerFactory serverContext;

    protected SslServerConfiguration(BuilderBase builder) {
        this.ciphers = builder.ciphers;
        this.keyFile = builder.keyFile;
        this.certFile = builder.certFile;
        this.allowPlaintext = builder.allowPlaintext;
    }

    protected final void initializeServerContext() {
        serverContext = createSslHandlerFactory();
    }

    protected abstract SslHandlerFactory createSslHandlerFactory();

    public SslHandler createHandler() throws Exception {
        return serverContext.newHandler();
    }
}
