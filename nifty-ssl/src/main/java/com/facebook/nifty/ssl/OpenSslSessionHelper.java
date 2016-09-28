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

import org.apache.tomcat.jni.SSL;
import org.jboss.netty.handler.ssl.OpenSslEngine;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;
import java.lang.reflect.Field;

/**
 * This class provides a method to extract properties of the SSL session
 * from an engine.
 * Netty's OpenSSL engine class does not implement getSession() fully, thus
 * we have to extract the properties that we need ourselves.
 */
public class OpenSslSessionHelper {
    private static Field sslField;

    static {
        try {
            sslField = OpenSslEngine.class.getDeclaredField("ssl");
            sslField.setAccessible(true);
        }
        catch (Throwable t) {
            // Ignore.
        }
    }

    public static SslSession getSession(SSLEngine sslEngine) throws SSLException {
        if (!(sslEngine instanceof OpenSslEngine)) {
            throw new IllegalArgumentException("ssl engine not openssl engine");
        }
        OpenSslEngine engine = (OpenSslEngine) sslEngine;
        if (sslField == null) {
            throw new SSLException("SSL field is null");
        }
        try {
            long sslPtr = (long) sslField.get(engine);
            if (sslPtr == 0) {
                throw new SSLException("SSL not initialized");
            }
            String alpn = SSL.getAlpnSelected(sslPtr);
            String npn = SSL.getNextProtoNegotiated(sslPtr);

            String version = SSL.getVersion(sslPtr);
            String cipher = SSL.getCipherForSSL(sslPtr);
            long establishedTime = SSL.getTime(sslPtr);

            // TODO: return the entire chain.
            // tc-native thinks that the chain is null, so we supply only the
            // leaf cert.
            byte[] cert = SSL.getPeerCertificate(sslPtr);
            X509Certificate certificate = null;
            if (cert != null) {
                certificate = X509Certificate.getInstance(cert);
            }
            return new SslSession(alpn, npn, version, cipher, establishedTime, certificate);
        }
        catch (IllegalAccessException e) {
            throw new SSLException(e);
        }
        catch (CertificateException e) {
            throw new SSLException(e);
        }
    }
}
