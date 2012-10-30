package com.facebook.swift.service.async;


import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftServer;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.thrift.TException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class AsyncClient extends AsyncTestBase
{
    protected ThriftServer syncServer;

    @Test
    public void testSyncClient()
            throws Exception
    {
        try (DelayedMap.Client client = createClient(DelayedMap.Client.class, syncServer)) {
            List<String> keys = Lists.newArrayList("testKey");
            List<String> values = client.getMultipleValues(0, TimeUnit.SECONDS, keys);
            assertEquals(values, Lists.newArrayList("default"));

            client.putValueSlowly(0, TimeUnit.SECONDS, "testKey", "testValue");
        }
    }

    @Test
    public void testAsyncClient()
            throws Exception
    {
        ListenableFuture<String> getBeforeFuture;
        ListenableFuture<String> getAfterFuture;
        ListenableFuture<Void> putFuture;

        try (DelayedMap.AsyncClient client = createClient(DelayedMap.AsyncClient.class, syncServer)) {
            getBeforeFuture = client.getValueSlowly(200, TimeUnit.MILLISECONDS, "testKey");
            putFuture = client.putValueSlowly(400, TimeUnit.MILLISECONDS, "testKey", "testValue");
            getAfterFuture = client.getValueSlowly(600, TimeUnit.MILLISECONDS, "testKey");

            assertEquals(Uninterruptibles.getUninterruptibly(getBeforeFuture), "default");
            assertEquals(Uninterruptibles.getUninterruptibly(getAfterFuture), "testValue");
            Uninterruptibles.getUninterruptibly(putFuture);
        }
    }

    private ThriftServer createSyncServer()
            throws InstantiationException, IllegalAccessException, TException
    {
        DelayedMapSyncHandler handler = new DelayedMapSyncHandler();
        handler.putValueSlowly(0, TimeUnit.MILLISECONDS, "testKey", "default");
        return createServerFromHandler(handler);
    }

    @BeforeMethod(alwaysRun = true)
    public void setup()
            throws IllegalAccessException, InstantiationException, TException
    {
        codecManager = new ThriftCodecManager();
        clientManager = new ThriftClientManager(codecManager);
        syncServer = createSyncServer();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown()
    {
        clientManager.close();
        syncServer.close();
    }
}
