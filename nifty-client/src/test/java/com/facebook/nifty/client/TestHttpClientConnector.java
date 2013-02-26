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
package com.facebook.nifty.client;

import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.testng.Assert.assertEquals;

public class TestHttpClientConnector
{
    @Test
    public void testBasicUri()
    {
        new HttpClientConnector(URI.create("http://good.com/foo"));
    }

    @Test
    public void testUriWithUppercaseValidScheme()
    {
        new HttpClientConnector(URI.create("HTTP://good.com/foo"));
    }

    @Test
    public void testUriWithHttpsScheme()
    {
        new HttpClientConnector(URI.create("https://good.com/foo"));
    }

    @Test
    public void testUriWithExplicitHttpPort()
    {
        new HttpClientConnector(URI.create("http://good.com:8000/foo"));
    }

    @Test
    public void testUriWithExplicitHttpsPort()
    {
        new HttpClientConnector(URI.create("https://good.com:80/foo"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testUriWithTooLongPortNumber()
    {
        new HttpClientConnector(URI.create("https://good.com:8888888/foo"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testUriWithInvalidScheme()
    {
        new HttpClientConnector(URI.create("ftp://good.com/foo"));
    }

    @Test
    public void testHostNameAndPortWithServicePath() throws URISyntaxException
    {
        HttpClientConnector connector = new HttpClientConnector("good.com:1234", "/foo");
        assertEquals(connector.toString(), "http://good.com:1234/foo");
    }

    @Test
    public void testHostNameAndDefaultPortWithServicePath() throws URISyntaxException
    {
        HttpClientConnector connector = new HttpClientConnector("good.com", "/foo");
        assertEquals(connector.toString(), "http://good.com/foo");
    }
}
