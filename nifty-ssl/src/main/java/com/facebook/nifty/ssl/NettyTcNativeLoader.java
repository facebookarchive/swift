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
import org.apache.tomcat.jni.Library;
import org.apache.tomcat.jni.SSL;
import org.jboss.netty.handler.ssl.OpenSsl;
import org.jboss.netty.util.internal.NativeLibraryLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Adds support for the Netty-tcnative uber jar. The current version of netty nifty runs with does not support the
 * uber jar. This should be removed when we move to new version of netty.
 * <p></p>
 * Most of this code is taken from
 * https://github.com/netty/netty/blob/379ad2c02ed0c0ae9f94e4081e3f910ece6380b7/handler/src/main/java/io/netty/handler/ssl/OpenSsl.java
 */
public class NettyTcNativeLoader {

    private static final String LINUX = "linux";
    private static final String UNKNOWN = "unknown";

    private static final Throwable UNAVAILABILITY_CAUSE;

    static {
        Throwable cause = null;
        try {
            loadTcNative();
        }
        catch (Throwable t) {
            cause = t;
        }
        UNAVAILABILITY_CAUSE = cause;
    }

    public static void ensureAvailable() {
        if (UNAVAILABILITY_CAUSE != null) {
            throw (Error) new UnsatisfiedLinkError(
                    "failed to load the required native library").initCause(UNAVAILABILITY_CAUSE);
        }
    }

    private static void loadTcNative() {
        String os = normalizeOs(System.getProperty("os.name", ""));
        String arch = normalizeArch(System.getProperty("os.arch", ""));

        Set<String> libNames = new LinkedHashSet<String>(3);
        // First, try loading the platform-specific library. Platform-specific
        // libraries will be available if using a tcnative uber jar.
        libNames.add("netty-tcnative-" + os + '-' + arch);
        if (LINUX.equalsIgnoreCase(os)) {
            // Fedora SSL lib so naming (libssl.so.10 vs libssl.so.1.0.0)..
            libNames.add("netty-tcnative-" + os + '-' + arch + "-fedora");
        }
        // finally the default library.
        libNames.add("netty-tcnative");

        boolean loaded = false;
        Throwable lastException = null;
        for (String libName : libNames) {
            try {
                NativeLibraryLoader.load(libName, SSL.class.getClassLoader());
                loaded = true;
                break;
            }
            catch (Throwable t) {
                lastException = t;
                continue;
            }
        }

        if (!loaded) {
            throw Throwables.propagate(lastException);
        }

        try {
            overrideExceptionValue();
            Library.initialize("provided");
            SSL.initialize(null);
        }
        catch (Throwable t) {
            throw Throwables.propagate(t);
        }
    }

    // Netty hardcodes the dependency to the Openssl class in such a way that it makes it difficult
    // to load tcnative outside netty.
    // This overrides netty's Openssl availablity to always report success since we are taking care of loading
    // the library.
    private static void overrideExceptionValue() {
        Field exceptionField = null;
        Field modifiersField = null;
        int originalModifiers = 0;
        boolean setModifiers = false;

        try {
            exceptionField = OpenSsl.class.getDeclaredField("UNAVAILABILITY_CAUSE");
            exceptionField.setAccessible(true);

            modifiersField = exceptionField.getClass().getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            originalModifiers = modifiersField.getInt(exceptionField);
            modifiersField.setInt(exceptionField, originalModifiers & ~Modifier.FINAL);
            setModifiers = true;
            // Override the default value. This will trigger the static block to run which tries to load the libs,
            // however we know that will fail.
            exceptionField.set(null, null);
        }
        catch (Throwable t) {
            throw Throwables.propagate(t);
        }
        finally {
            // Restore the fields to their original values
            if (exceptionField != null && modifiersField != null & setModifiers) {
                try {
                    modifiersField.setInt(exceptionField, originalModifiers);
                }
                catch (IllegalAccessException e) {
                    // Allow the original exception to propagate instead.
                    return;
                }
            }
        }
    }

    private static String normalizeOs(String value) {
        value = normalize(value);
        if (value.startsWith("aix")) {
            return "aix";
        }
        if (value.startsWith("hpux")) {
            return "hpux";
        }
        if (value.startsWith("os400")) {
            // Avoid the names such as os4000
            if (value.length() <= 5 || !Character.isDigit(value.charAt(5))) {
                return "os400";
            }
        }
        if (value.startsWith(LINUX)) {
            return LINUX;
        }
        if (value.startsWith("macosx") || value.startsWith("osx")) {
            return "osx";
        }
        if (value.startsWith("freebsd")) {
            return "freebsd";
        }
        if (value.startsWith("openbsd")) {
            return "openbsd";
        }
        if (value.startsWith("netbsd")) {
            return "netbsd";
        }
        if (value.startsWith("solaris") || value.startsWith("sunos")) {
            return "sunos";
        }
        if (value.startsWith("windows")) {
            return "windows";
        }

        return UNKNOWN;
    }

    private static String normalizeArch(String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "x86_64";
        }
        if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x86_32";
        }
        if (value.matches("^(ia64|itanium64)$")) {
            return "itanium_64";
        }
        if (value.matches("^(sparc|sparc32)$")) {
            return "sparc_32";
        }
        if (value.matches("^(sparcv9|sparc64)$")) {
            return "sparc_64";
        }
        if (value.matches("^(arm|arm32)$")) {
            return "arm_32";
        }
        if ("aarch64".equals(value)) {
            return "aarch_64";
        }
        if (value.matches("^(ppc|ppc32)$")) {
            return "ppc_32";
        }
        if ("ppc64".equals(value)) {
            return "ppc_64";
        }
        if ("ppc64le".equals(value)) {
            return "ppcle_64";
        }
        if ("s390".equals(value)) {
            return "s390_32";
        }
        if ("s390x".equals(value)) {
            return "s390_64";
        }

        return UNKNOWN;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }
}
