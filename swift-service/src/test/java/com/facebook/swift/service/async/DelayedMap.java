package com.facebook.swift.service.async;

import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TException;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DelayedMap
{

    @ThriftService("DelayedMap")
    public static interface Service
    {
        @ThriftMethod
        public String getValueSlowly(long timeout, TimeUnit unit, String key)
                throws TException;

        @ThriftMethod
        public void putValueSlowly(long timeout, TimeUnit unit, String key, String value)
                throws TException;

        @ThriftMethod
        public List<String> getMultipleValues(long timeout, TimeUnit unit, List<String> keys)
                throws TException;
    }

    @ThriftService("DelayedMap")
    public static interface AsyncService
    {
        @ThriftMethod
        public ListenableFuture<String> getValueSlowly(long timeout, TimeUnit unit, String key)
                throws TException;

        @ThriftMethod
        public ListenableFuture<Void> putValueSlowly(long timeout, TimeUnit unit, String key, String value)
                throws TException;

        @ThriftMethod
        public ListenableFuture<List<String>> getMultipleValues(long timeout, TimeUnit unit, List<String> keys)
                throws TException;
    }


    @ThriftService("DelayedMap")
    public interface Client extends Closeable
    {
        @ThriftMethod
        public String getValueSlowly(long timeout, TimeUnit unit, String key)
                throws TException;

        @ThriftMethod
        public Void putValueSlowly(long timeout, TimeUnit unit, String key, String value)
                throws TException;

        @ThriftMethod
        public List<String> getMultipleValues(long timeout, TimeUnit unit, List<String> keys)
                throws TException;
    }

    @ThriftService("DelayedMap")
    public interface AsyncClient extends Closeable
    {
        @ThriftMethod
        public ListenableFuture<String> getValueSlowly(long timeout, TimeUnit unit,
                                                            String key)
                throws TException;

        @ThriftMethod
        public ListenableFuture<Void> putValueSlowly(long timeout, TimeUnit unit, String key, String value)
                throws TException;

        @ThriftMethod
        public ListenableFuture<List<String>> getMultipleValues(long timeout, TimeUnit unit, List<String> keys)
                throws TException;
    }
}
