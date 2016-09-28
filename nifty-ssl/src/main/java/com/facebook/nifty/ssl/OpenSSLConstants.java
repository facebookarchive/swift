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

/**
 * Collection of constants defined by OpenSSL which are not available via
 * netty tc-native.
 */
public interface OpenSSLConstants {
    /* Session related */
    long SSL_SESS_CACHE_NO_INTERNAL_LOOKUP = 0x0100;
    long SSL_SESS_CACHE_NO_INTERNAL_STORE = 0x0200;
    long SSL_SESS_CACHE_NO_INTERNAL = SSL_SESS_CACHE_NO_INTERNAL_LOOKUP | SSL_SESS_CACHE_NO_INTERNAL_STORE;
}
