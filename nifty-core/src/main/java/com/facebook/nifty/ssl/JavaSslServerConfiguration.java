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
import org.jboss.netty.handler.ssl.SslContext;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.ssl.SslProvider;

import javax.net.ssl.SSLException;

public class JavaSslServerConfiguration extends SslServerConfiguration {

    private JavaSslServerConfiguration(BuilderBase builder) {
        super(builder);
    }

    public static class Builder extends BuilderBase<Builder> {
        @Override
        protected SslServerConfiguration createServerConfiguration() {
            SslServerConfiguration sslServerConfiguration = new JavaSslServerConfiguration(this);
            sslServerConfiguration.initializeServerContext();
            return sslServerConfiguration;
        }
    }

    public static JavaSslServerConfiguration.Builder newBuilder() {
        return new JavaSslServerConfiguration.Builder();
    }

    protected SslHandlerFactory createSslHandlerFactory() {
        try {
            SslContext sslHandler =
                    SslContext.newServerContext(
                    SslProvider.JDK,
                    null,
                    certFile,
                    keyFile,
                    null,
                    ciphers,
                    null,
                    0,
                    0);
            return new SslHandlerFactory() {
                @Override
                public SslHandler newHandler() {
                    return sslHandler.newHandler();
                }
            };
        }
        catch (SSLException e) {
            throw Throwables.propagate(e);
        }
    }
}
