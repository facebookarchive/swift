package com.facebook.swift.service.async;

import com.facebook.swift.service.async.DelayedMap;
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
