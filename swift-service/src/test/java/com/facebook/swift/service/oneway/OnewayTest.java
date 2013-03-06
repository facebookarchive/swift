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
package com.facebook.swift.service.oneway;

import com.facebook.swift.service.base.SuiteBase;
import org.apache.thrift.TException;
import org.testng.annotations.Test;

@Test
public class OnewayTest extends SuiteBase<OneWayService, OneWayService>
{

    public OnewayTest()
    {
        super(OneWayServiceHandler.class, OneWayServiceClient.class);
    }

    @Test
    public void testOnewayCall()
            throws TException
    {
        getClient().onewayMethod();
        getClient().verifyConnectionState();
    }

    @Test
    public void testOneWayThrow()
            throws TException, OneWayException
    {
        getClient().onewayThrow();
        getClient().verifyConnectionState();
    }

}
