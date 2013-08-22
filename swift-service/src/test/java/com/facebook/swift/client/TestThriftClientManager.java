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
package com.facebook.swift.client;

import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.async.DelayedMap;
import com.facebook.swift.service.async.DelayedMapSyncHandler;
import com.facebook.swift.service.base.SuiteBase;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class TestThriftClientManager extends SuiteBase<DelayedMap.Service, DelayedMap.Client>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TestThriftClientManager.class);
    public static final String LOCALHOST_IP_ADDRESS = "127.0.0.1";

    public TestThriftClientManager()
    {
        super(DelayedMapSyncHandler.class, DelayedMap.Client.class, new ThriftServerConfig().setBindAddress(LOCALHOST_IP_ADDRESS));
    }

    @BeforeTest
    public void setup()
    {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
    }

    @Test
    public void testUnresolvedRemoteAddress()
    {
        // Test that getRemoteAddress on a client that connected to '127.0.0.1' does not resolve the IP to 'localhost'
        // (because doing a reverse lookup causes performance problems - e.g. for logging code)
        assertTrue(getClientManager().getRemoteAddress(getClient()).toString().startsWith(LOCALHOST_IP_ADDRESS));
    }
}
