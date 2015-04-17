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
package com.facebook.swift.service.server;

import com.facebook.nifty.core.NettyServerConfig;
import com.facebook.nifty.core.ThriftServerDef;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftEventHandler;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftService;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.testng.Assert.assertTrue;

public class TestThriftServer
{
    // Test that normal shutdown is fast
    @Test
    public void testThriftServerShutdown()
            throws InterruptedException
    {
        ServerCreator serverCreator = new ServerCreator().invoke();
        ThriftServer server = serverCreator.getServer();

        server.start();
        serverCreator.stop();
        serverCreator.checkExecutorsTerminated();
    }

    // Test that shutdown works even if you didn't start the server
    @Test
    public void testThriftServerShutdownWithNoStartup()
            throws InterruptedException
    {
        ServerCreator serverCreator = new ServerCreator().invoke();

        serverCreator.stop();
        serverCreator.checkExecutorsTerminated();
    }

    @ThriftService
    public class SimpleService
    {
        @ThriftMethod
        public void sleep(int seconds)
        {
            Uninterruptibles.sleepUninterruptibly(seconds, TimeUnit.SECONDS);
        }
    }

    private class ServerCreator
    {
        private ExecutorService taskWorkerExecutor;
        private ThriftServer server;
        private CountDownLatch latch;
        private ExecutorService bossExecutor;
        private ExecutorService ioWorkerExecutor;

        public ThriftServer getServer()
        {
            return server;
        }

        public ServerCreator invoke()
        {
            ThriftServiceProcessor processor = new ThriftServiceProcessor(
                    new ThriftCodecManager(),
                    ImmutableList.<ThriftEventHandler>of(),
                    new SimpleService()
            );

            taskWorkerExecutor = newFixedThreadPool(1);

            ThriftServerDef serverDef = ThriftServerDef.newBuilder()
                                                       .listen(0)
                                                       .withProcessor(processor)
                                                       .using(taskWorkerExecutor)
                                                       .build();

            bossExecutor = newCachedThreadPool();
            ioWorkerExecutor = newCachedThreadPool();

            NettyServerConfig serverConfig = NettyServerConfig.newBuilder()
                                                              .setBossThreadExecutor(bossExecutor)
                                                              .setWorkerThreadExecutor(ioWorkerExecutor)
                                                              .build();

            server = new ThriftServer(serverConfig, serverDef);
            return this;
        }

        public void checkExecutorsTerminated()
        {
            assertTrue(bossExecutor.isTerminated());
            assertTrue(ioWorkerExecutor.isTerminated());
            assertTrue(taskWorkerExecutor.isTerminated());
        }

        public void stop() {
            server.close();
        }
    }
}
