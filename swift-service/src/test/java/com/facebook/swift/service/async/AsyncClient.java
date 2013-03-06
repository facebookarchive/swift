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
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
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
    void testAsyncConnection() throws Exception
    {
        ListenableFuture<DelayedMap.AsyncClient> future = createClient(DelayedMap.AsyncClient.class, syncServer);
        AsyncConnectionCallback futureCallback = new AsyncConnectionCallback();

        Futures.addCallback(future, futureCallback);
        futureCallback.waitForAsyncCallsToBeDispatched();
        futureCallback.checkAsyncCallReturnValues();
    }

    /* Helper class for testAsyncConnection() */
    class AsyncConnectionCallback implements FutureCallback<DelayedMap.AsyncClient> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private ListenableFuture<String> getBeforeFuture = null;
        private ListenableFuture<String> getAfterFuture = null;
        private ListenableFuture<Void> putFuture = null;

        @Override
        public void onSuccess(DelayedMap.AsyncClient client)
        {
            try {
                getBeforeFuture = client.getValueSlowly(200, TimeUnit.MILLISECONDS, "testKey");
                putFuture = client.putValueSlowly(400, TimeUnit.MILLISECONDS, "testKey", "testValue");
                getAfterFuture = client.getValueSlowly(600, TimeUnit.MILLISECONDS, "testKey");
            }
            catch (Throwable t) {
                onFailure(t);
            }

            latch.countDown();
        }

        @Override
        public void onFailure(Throwable t)
        {
            latch.countDown();
        }

        public void waitForAsyncCallsToBeDispatched() throws InterruptedException
        {
            latch.await();
        }

        public void checkAsyncCallReturnValues() throws ExecutionException, InterruptedException
        {
            // All these async calls include a delay, so none she be finished at first
            assertFalse(getBeforeFuture.isDone());
            assertFalse(getAfterFuture.isDone());
            assertFalse(putFuture.isDone());

            // Calls are timed to complete in order, but Verify that we can still wait on the
            // futures out of order
            assertEquals(getBeforeFuture.get(), "default");
            assertEquals(getAfterFuture.get(), "testValue");
            putFuture.get();
        }
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
        try (DelayedMap.AsyncClient client = createClient(DelayedMap.AsyncClient.class, syncServer).get()) {
            ListenableFuture<String> getFuture = client.getValueSlowly(1500, TimeUnit.MILLISECONDS, "testKey");
            try {
                getFuture.get(2000, TimeUnit.MILLISECONDS);
                fail("Call did not timeout as expected");
            }
            catch (java.util.concurrent.TimeoutException e) {
                fail("Waited too long for channel timeout");
            }
            catch (ExecutionException e) {
                checkTransportException(e.getCause(), ReadTimeoutException.class);
            }
        }
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

    /**
     * Verify that a {@link Throwable} is a {@link TTransportException} wrapping the expected cause
     * @param throwable The {@link Throwable} to check
     * @param expectedCause The expected cause of the {@link TTransportException}
     */
    private void checkTransportException(Throwable throwable, Class<? extends Throwable> expectedCause)
    {
        assertNotNull(throwable);
        Throwable cause = throwable.getCause();

        if (!(throwable instanceof TTransportException)) {
            fail("Exception of type " + throwable.getClass() + " when expecting a TTransportException");
        }
        else if (!(expectedCause.isAssignableFrom(throwable.getCause().getClass()))) {
            fail("TTransportException caused by " + cause.getClass() +
                 " when expecting a TTransportException caused by " + expectedCause);
        }
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
        syncServer.close();
        clientManager.close();
    }
}
