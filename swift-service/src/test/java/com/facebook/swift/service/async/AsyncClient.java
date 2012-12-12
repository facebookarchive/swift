package com.facebook.swift.service.async;

import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftServer;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.handler.timeout.TimeoutException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class AsyncClient extends AsyncTestBase
{
    protected ThriftServer syncServer;

    @Test
    public void testSyncClient()
            throws Exception
    {
        try (DelayedMap.Client client = createClient(DelayedMap.Client.class, syncServer).get()) {
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

        try (DelayedMap.AsyncClient client = createClient(DelayedMap.AsyncClient.class, syncServer).get()) {
            getBeforeFuture = client.getValueSlowly(200, TimeUnit.MILLISECONDS, "testKey");
            putFuture = client.putValueSlowly(400, TimeUnit.MILLISECONDS, "testKey", "testValue");
            getAfterFuture = client.getValueSlowly(600, TimeUnit.MILLISECONDS, "testKey");

            assertEquals(Uninterruptibles.getUninterruptibly(getBeforeFuture), "default");
            assertEquals(Uninterruptibles.getUninterruptibly(getAfterFuture), "testValue");
            Uninterruptibles.getUninterruptibly(putFuture);
        }
    }

    @Test(timeOut = 2000)
    void testAsyncConnection()
            throws Exception
    {
        DelayedMap.AsyncClient client = null;
        final CountDownLatch latch = new CountDownLatch(1);

        ListenableFuture<DelayedMap.AsyncClient> future = createClient(DelayedMap.AsyncClient.class, syncServer);
        Futures.addCallback(future, new FutureCallback<DelayedMap.AsyncClient>()
        {
            @Override
            public void onSuccess(DelayedMap.AsyncClient client)
            {
                ListenableFuture<String> getBeforeFuture;
                ListenableFuture<String> getAfterFuture;
                ListenableFuture<Void> putFuture;

                try {
                    try {
                        getBeforeFuture = client.getValueSlowly(200, TimeUnit.MILLISECONDS, "testKey");
                        putFuture = client.putValueSlowly(400, TimeUnit.MILLISECONDS, "testKey", "testValue");
                        getAfterFuture = client.getValueSlowly(600, TimeUnit.MILLISECONDS, "testKey");

                        assertEquals(Uninterruptibles.getUninterruptibly(getBeforeFuture), "default");
                        assertEquals(Uninterruptibles.getUninterruptibly(getAfterFuture), "testValue");
                        Uninterruptibles.getUninterruptibly(putFuture);
                    } finally {
                        client.close();
                    }
                } catch (Throwable t) {
                    onFailure(t);
                }

                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t)
            {
                Throwables.propagate(t);
                latch.countDown();
            }
        });

        latch.await();
    }

    @Test
    public void testAsyncOutOfOrder()
            throws Exception
    {
        ListenableFuture<String> getFuture;
        ListenableFuture<Void> putFuture;

        try (DelayedMap.AsyncClient client = createClient(DelayedMap.AsyncClient.class, syncServer).get()) {
            getFuture = client.getValueSlowly(500, TimeUnit.MILLISECONDS, "testKey");
            putFuture = client.putValueSlowly(250, TimeUnit.MILLISECONDS, "testKey", "testValue");

            assertEquals(getFuture.get(1, TimeUnit.SECONDS), "testValue");
            putFuture.get(1, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testAsyncEarlyListener()
            throws Exception
    {
        ListenableFuture<String> getFuture;
        final CountDownLatch latch = new CountDownLatch(1);

        try (DelayedMap.AsyncClient client = createClient(DelayedMap.AsyncClient.class, syncServer).get()) {
            getFuture = client.getValueSlowly(500, TimeUnit.MILLISECONDS, "testKey");

            // Add callback immediately, to test case where it should be fired later when the
            // results are ready
            Futures.addCallback(getFuture, new FutureCallback<String>()
            {
                @Override
                public void onSuccess(String result)
                {
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable t)
                {
                }
            });

            latch.await();
        }
    }

    // Call a method that will sleep for longer than the channel timeout, and expect a
    // TimeoutException (wrapped in a TTransportException)
    @Test
    public void testAsyncTimeout()
            throws Exception
    {
        ListenableFuture<String> getFuture;
        final CountDownLatch latch = new CountDownLatch(1);

        try (DelayedMap.AsyncClient client = createClient(DelayedMap.AsyncClient.class, syncServer).get()) {
            getFuture = client.getValueSlowly(1500, TimeUnit.MILLISECONDS, "testKey");
            Futures.addCallback(getFuture, new FutureCallback<String>()
            {
                @Override
                public void onSuccess(String result)
                {
                    fail("Successful result received when timeout was expected");
                }

                @Override
                public void onFailure(Throwable t)
                {
                    assert(t instanceof TTransportException &&
                           t.getCause() instanceof TimeoutException);
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS), "Waited too long for timeout");
    }

    @Test
    public void testAsyncLateListener()
            throws Exception
    {
        ListenableFuture<String> getFuture;
        final CountDownLatch latch = new CountDownLatch(1);

        try (DelayedMap.AsyncClient client = createClient(DelayedMap.AsyncClient.class, syncServer).get()) {
            getFuture = client.getValueSlowly(250, TimeUnit.MILLISECONDS, "testKey");

            // Sleep first, to test the case where the callback is executed immediately
            Thread.sleep(500);

            Futures.addCallback(getFuture, new FutureCallback<String>()
            {
                @Override
                public void onSuccess(String result)
                {
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable t)
                {
                }
            });

            // Because of the sleep above, the latch should already be open
            // (because the callback is added on this thread and the default callback
            // executor runs callbacks on the same thread, this shouldn't even be a race).
            latch.await(0, TimeUnit.MILLISECONDS);
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
