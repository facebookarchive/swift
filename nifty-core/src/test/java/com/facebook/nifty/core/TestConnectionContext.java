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
package com.facebook.nifty.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.thrift.TException;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.facebook.nifty.processor.NiftyProcessor;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

public class TestConnectionContext extends AbstractLiveTest
{

    @Test
    public void testContextNormal() throws IOException, TException, InterruptedException
    {
        final SynchronousQueue<RequestContext> requestContextQueue = new SynchronousQueue<>();
        final SynchronousQueue<SettableFuture<Boolean>> sendResponseQueue = new SynchronousQueue<>();
        NiftyProcessor processor = mockProcessor(null, null, requestContextQueue, sendResponseQueue);

        try (FakeServer server = listen(processor);
             FakeClient client = connect(server)) {

            // Issue a fake request and wait for it to arrive
            client.sendRequest();
            RequestContext requestContext = requestContextQueue.poll(30, TimeUnit.SECONDS);
            Preconditions.checkNotNull(requestContext, "Either deadlock, or your computer is really slow");
            ConnectionContext actualContext = requestContext.getConnectionContext();
            SettableFuture<Boolean> sendResponse = sendResponseQueue.take();

            // ConnectionContext should show the request from the client
            Assert.assertNotNull(
                    actualContext.getRemoteAddress(),
                    "remote address non-null");
            Assert.assertEquals(
                    ((InetSocketAddress) actualContext.getRemoteAddress()).getPort(),
                    client.getClientPort(),
                    "context has correct port");

            sendResponse.set(false);
        }
    }

    @Test
    public void testContextOnClosedConnection() throws IOException, TException, InterruptedException
    {
        // An ExecutorService which lets us delay calls from NiftyDispatcher to the processor
        final SynchronousQueue<Semaphore> tasksWaitingToRun = new SynchronousQueue<>();
        final ExecutorService threadpool = Executors.newCachedThreadPool();
        Executor slowExecutor = new Executor() {
            @Override
            public void execute(final Runnable task) {
                threadpool.execute(new Runnable() {
                    @Override
                    public void run() {
                        Semaphore allowMeToContinue = new Semaphore(0);
                        Uninterruptibles.putUninterruptibly(tasksWaitingToRun, allowMeToContinue);
                        allowMeToContinue.acquireUninterruptibly();
                        task.run();
                    }
                });
            }
        };

        final SynchronousQueue<RequestContext> requestContextQueue = new SynchronousQueue<>();
        final SynchronousQueue<SettableFuture<Boolean>> sendResponseQueue = new SynchronousQueue<>();
        NiftyProcessor processor = mockProcessor(null, null, requestContextQueue, sendResponseQueue);

        ChannelGroup channels = new DefaultChannelGroup();

        try (FakeServer server = listen(processor, slowExecutor, channels);
             FakeClient client = connect(server)) {

            // Send a request, and wait for NiftyDispatcher to put it in the queue
            client.sendRequest();
            Semaphore allowNiftyProcessorToRun = tasksWaitingToRun.poll(30, TimeUnit.SECONDS);
            Preconditions.checkNotNull(allowNiftyProcessorToRun, "Either deadlock, or your computer is really slow");

            // Close the channel. We do the close on the server side because that
            // lets us control when all the close handlers are run (namely, on
            // our thread) which in turn lets us wait until the channel has finished
            // closing.
            final AtomicReference<Thread> threadThatProcessedClose = new AtomicReference<>();
            Channel channelToClient = Iterables.getOnlyElement(channels);
            channelToClient.getCloseFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    threadThatProcessedClose.set(Thread.currentThread());
                }
            });
            channelToClient.close();
            Preconditions.checkState(threadThatProcessedClose.get() == Thread.currentThread());

            // Now allow the NiftyProcessor to run, and wait for it to finish
            allowNiftyProcessorToRun.release();
            RequestContext requestContext = requestContextQueue.poll(30, TimeUnit.SECONDS);
            Preconditions.checkNotNull(requestContext, "Either deadlock, or your computer is really slow");
            ConnectionContext actualContext = requestContext.getConnectionContext();
            SettableFuture<Boolean> sendResponse = sendResponseQueue.take();

            // The connection context should still be correct
            Assert.assertNotNull(
                    actualContext.getRemoteAddress(),
                    "remote address non-null");
            Assert.assertEquals(
                    ((InetSocketAddress) actualContext.getRemoteAddress()).getPort(),
                    client.getClientPort(),
                    "context has correct port");

            sendResponse.set(false);
        } finally {
            threadpool.shutdown();
        }
    }


}
