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
