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
import org.jboss.netty.handler.ssl.SslProvider;

import javax.net.ssl.SSLException;
import java.io.File;

public class JDKSSLServerConfiguration extends SSLServerConfiguration {

    private JDKSSLServerConfiguration(BuilderBase builder) {
        super(builder);
    }

    public static class Builder extends BuilderBase<Builder> {
        @Override
        protected SSLServerConfiguration createServerConfiguration() {
            SSLServerConfiguration sslServerConfiguration = new JDKSSLServerConfiguration(this);
            sslServerConfiguration.initializeServerContext();
            return sslServerConfiguration;
        }
    }

    public static JDKSSLServerConfiguration.Builder newBuilder() {
        return new JDKSSLServerConfiguration.Builder();
    }

    protected SslContext createServerContext() {
        try {
            return SslContext.newServerContext(
                            SslProvider.JDK,
                            null,
                            certFile,
                            keyFile,
                            null,
                            ciphers,
                            null,
                            0,
                            0);
        } catch (SSLException e) {
            throw Throwables.propagate(e);
        }
    }
}
