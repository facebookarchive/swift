package com.facebook.swift.service.async;

import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftServer;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class AsyncService extends AsyncTestBase
{

    private ThriftServer asyncServer;

    @Test(timeOut = 1000)
    public void testAsyncService()
            throws Exception
    {
        try (DelayedMap.Client client = createClient(DelayedMap.Client.class, asyncServer)) {
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
