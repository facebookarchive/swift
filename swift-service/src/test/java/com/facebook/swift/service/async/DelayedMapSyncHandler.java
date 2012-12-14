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

import org.apache.thrift.TException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DelayedMapSyncHandler implements DelayedMap.Service
{
    private Map<String, String> store = new HashMap<>();

    @Override
    public void putValueSlowly(long timeout,
                               TimeUnit unit,
                               String key,
                               String value)
            throws TException
    {
        checkedSleep(timeout, unit);
        store.put(key, value);
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

    private void checkedSleep(long timeout, TimeUnit unit)
    {
        try {
            Thread.sleep(unit.toMillis(timeout));
        }
        catch (InterruptedException ignored) {
        }
    }
}
