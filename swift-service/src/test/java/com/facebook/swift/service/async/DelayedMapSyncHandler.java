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

import io.airlift.log.Logger;
import org.apache.thrift.TException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Maps.newConcurrentMap;

public class DelayedMapSyncHandler implements DelayedMap.Service
{
    private static final Logger LOGGER = Logger.get(DelayedMapSyncHandler.class);

    private Map<String, String> store = newConcurrentMap();

    @Override
    public void putValueSlowly(long timeout,
                               TimeUnit unit,
                               String key,
                               String value)
            throws TException
    {
        LOGGER.info("put started");
        checkedSleep(timeout, unit);
        store.put(key, value);
        LOGGER.info("put finished");
    }

    @Override
    public String getValueSlowly(long timeout,
                                 TimeUnit unit,
                                 String key)
            throws TException
    {
        checkedSleep(timeout, unit);
        return store.get(key);
    }

    @Override
    public List<String> getMultipleValues(long timeout, TimeUnit unit, List<String> keys)
            throws TException
    {
        checkedSleep(timeout, unit);
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            result.add(store.get(key));
        }
        return result;
    }

    @Override
    public void onewayPutValueSlowly(long timeout, TimeUnit unit, String key, String value)
    {
        LOGGER.info("oneway put started");
        checkedSleep(timeout, unit);
        store.put(key, value);
        LOGGER.info("oneway put finished");
    }

    private void checkedSleep(long timeout, TimeUnit unit)
    {
        try {
            Thread.sleep(unit.toMillis(timeout));
        }
        catch (InterruptedException ignored) {
        }
    }
}
