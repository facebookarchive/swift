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
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftServer;
import com.google.common.util.concurrent.ListenableFuture;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.testng.Assert.assertEquals;

public class AsyncService extends AsyncTestBase
{

    private static final int TARGET_SERVER_THREAD_COUNT = 30;
    private static final int PROXY_SERVER_THREAD_COUNT = 1;

    private ThriftServer asyncServer;
    private ThriftServer syncServer;

    @Test(timeOut = 1000)
    public void testAsyncService()
            throws Exception
    {
        try (DelayedMap.AsyncClient client = createClient(DelayedMap.AsyncClient.class, asyncServer).get()) {
            List<String> keys = newArrayList();
            Set<String> expectedValues = newHashSet();

            List<ListenableFuture<Void>> putFutures = newArrayList();

            // Proxy server has only one worker threads, so if it executes these requests
            // sequentially, it should timeout. The handler is async though, and proxies the requests
            // to a server with many threads, so all these requests should be handled near-simultaneously
            for (int i = 0; i < TARGET_SERVER_THREAD_COUNT; i++) {
                String key = "key" + Integer.toString(i);
                String value = "value" + Integer.toString(i);
                keys.add(key);
                expectedValues.add(value);
                putFutures.add(i, client.putValueSlowly(200, TimeUnit.MILLISECONDS, key, value));
            }

            // Wait for all puts to finish
            for (int i = 0; i < TARGET_SERVER_THREAD_COUNT; i++) {
                putFutures.get(i).get();
            }

            List<String> values = client.getMultipleValues(100, TimeUnit.MILLISECONDS, keys).get();
            assertEquals(newHashSet(values), expectedValues);
        }
    }

    @BeforeMethod(alwaysRun = true)
    private void setup() throws Exception
    {
        codecManager = new ThriftCodecManager();
        clientManager = new ThriftClientManager(codecManager);
        syncServer = createTargetServer(TARGET_SERVER_THREAD_COUNT);
        asyncServer = createAsyncServer(PROXY_SERVER_THREAD_COUNT, clientManager, syncServer);
    }

    @AfterMethod(alwaysRun = true)
    private void tearDown()
    {
        clientManager.close();
        asyncServer.close();
    }
}
