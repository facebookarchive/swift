/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.service.async;

import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.LogEntry;
import com.facebook.swift.service.ResultCode;
import com.facebook.swift.service.Scribe;
import com.facebook.swift.service.ThriftClientManager;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;

import static org.testng.Assert.assertEquals;

public class AsyncHttpClientTest extends AsyncTestBase
{
    // Test a simple sync client call through HTTP (in this case, to a TServlet running under Jetty)
    @Test
    public void testHttpClient()
            throws Exception
    {
        try (HttpScribeServer server = new HttpScribeServer())
        {
            server.start();

            // Server was just started, it shouldn't have recorded any messages yet
            assertEquals(server.getLogEntries().size(), 0);

            int serverPort = server.getLocalPort();

            try (Scribe client = createHttpClient(Scribe.class, serverPort).get()) {
                client.log(Lists.newArrayList(new LogEntry("testCategory", "testMessage")));
            }

            // Blocking call completed, check that it was successful in logging a message.
            assertEquals(server.getLogEntries().size(), 1);
        }
    }

    // Test a simple async client call to the same servlet
    @Test
    public void testHttpAsyncClient()
            throws Exception
    {
        try (final HttpScribeServer server = new HttpScribeServer())
        {
            server.start();
            final CountDownLatch latch = new CountDownLatch(1);

            // Server was just started, it shouldn't have recorded any messages yet
            assertEquals(server.getLogEntries().size(), 0);

            int serverPort = server.getLocalPort();

            ListenableFuture<AsyncScribe> clientFuture = createHttpClient(AsyncScribe.class, serverPort);
            Futures.addCallback(clientFuture, new FutureCallback<AsyncScribe>()
            {
                @Override
                public void onSuccess(AsyncScribe client)
                {
                    try {
                        ListenableFuture<ResultCode> methodFuture = client.log(Lists.newArrayList(new LogEntry("testCategory", "testMessage")));

                        // Connected a client, and made an async call against it, but the call shouldn't
                        // be completed right away. Check that it isn't.
                        assertEquals(server.getLogEntries().size(), 0);

                        Futures.addCallback(methodFuture, new FutureCallback<ResultCode>()
                        {
                            @Override
                            public void onSuccess(ResultCode result)
                            {
                                latch.countDown();
                            }

                            @Override
                            public void onFailure(Throwable t)
                            {
                                latch.countDown();
                            }
                        });
                    }
                    catch (Throwable th) {
                        onFailure(th);
                    }
                }

                @Override
                public void onFailure(Throwable t)
                {
                    latch.countDown();
                }
            });

            latch.await();

            // Now that the latch is clear, client should have connected and the async call completed
            // so check that it did so successfully.
            assertEquals(server.getLogEntries().size(), 1);
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setup()
            throws IllegalAccessException, InstantiationException, TException
    {
        codecManager = new ThriftCodecManager();
        clientManager = new ThriftClientManager(codecManager);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown()
    {
        clientManager.close();
    }
}
