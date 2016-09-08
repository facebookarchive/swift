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

import org.jboss.netty.handler.ssl.OpenSslEngine;
import org.jboss.netty.handler.ssl.SslBufferPool;
import org.jboss.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

/**
 * We're seeing SslEngine leaking in a few places.
 * This SslHandler uses a finalizer to clean up the SslEngine
 * correctly like netty 4 does as well.
 */
public class BetterSslHandler extends SslHandler {

    private final OpenSslEngine sslEngine;

    public BetterSslHandler(SSLEngine engine, SslBufferPool bufferPool) {
        super(engine, bufferPool);
        sslEngine = (OpenSslEngine) engine;
    }

    @Override
    protected void finalize() throws Throwable {
        sslEngine.shutdown();
        super.finalize();
    }
}
