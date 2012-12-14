/**
 * Copyright 2012 Facebook, Inc.
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
import com.google.common.collect.Lists;
import org.apache.thrift.TException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class AsyncService extends AsyncTestBase
{

    private ThriftServer asyncServer;

    @Test(timeOut = 1000)
    public void testAsyncService()
            throws Exception
    {
        try (DelayedMap.Client client = createClient(DelayedMap.Client.class, asyncServer).get()) {
            List<String> keys = Lists.newArrayList("testKey");
            List<String> values = client.getMultipleValues(0, TimeUnit.SECONDS, keys);
            assertEquals(values, Lists.newArrayList("default"));
        }
    }

    @BeforeMethod(alwaysRun = true)
    private void setup()
            throws IllegalAccessException, InstantiationException, TException
    {
        codecManager = new ThriftCodecManager();
        clientManager = new ThriftClientManager(codecManager);
        asyncServer = createAsyncServer();
    }

    @AfterMethod(alwaysRun = true)
    private void tearDown()
    {
        clientManager.close();
        asyncServer.close();
    }
}
