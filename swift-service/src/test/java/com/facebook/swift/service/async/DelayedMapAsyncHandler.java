package com.facebook.swift.service.async;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DelayedMapAsyncHandler implements DelayedMap.AsyncService
{
    private DelayedMapSyncHandler innerHandler = new DelayedMapSyncHandler();

    @Override
    public ListenableFuture<Void> putValueSlowly(long timeout,
                               TimeUnit unit,
                               String key,
                               String value)
            throws TException
    {
        innerHandler.putValueSlowly(timeout, unit, key, value);
        return Futures.immediateFuture((Void)null);
    }

    @Override
    public ListenableFuture<String> getValueSlowly(long timeout,
                                 TimeUnit unit,
                                 String key)
            throws TException
    {
        return Futures.immediateFuture(innerHandler.getValueSlowly(timeout, unit, key));
    }

    @Override
    public ListenableFuture<List<String>> getMultipleValues(long timeout, TimeUnit unit, List<String> keys)
            throws TException
    {
        return Futures.immediateFuture(innerHandler.getMultipleValues(timeout, unit, keys));
    }
}
