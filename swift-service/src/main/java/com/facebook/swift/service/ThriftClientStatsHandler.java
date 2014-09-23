/*
 * Copyright (C) 2013 Facebook, Inc.
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
package com.facebook.swift.service;

import com.facebook.nifty.client.ClientRequestContext;
import io.airlift.units.Duration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static io.airlift.units.Duration.nanosSince;
import static java.lang.System.nanoTime;

public class ThriftClientStatsHandler extends ThriftClientEventHandler
{
    private final ConcurrentHashMap<String, ThriftMethodStats> stats = new ConcurrentHashMap<>();

    private static class PerCallMethodStats
    {
        public boolean success = true;
        public long startTime = nanoTime();
        public long preReadTime;
        public long preWriteTime;
        public long postWriteTime;
    }

    private static Duration nanosBetween(long start, long end)
    {
        return new Duration(end - start, TimeUnit.NANOSECONDS);
    }

    public ConcurrentMap<String, ThriftMethodStats> getStats()
    {
        return stats;
    }

    @Override
    public Object getContext(String methodName, ClientRequestContext requestContext)
    {
        stats.putIfAbsent(methodName, new ThriftMethodStats());
        return new PerCallMethodStats();
    }

    @Override
    public void preWrite(Object context, String methodName, Object[] args)
    {
        ((PerCallMethodStats)context).preWriteTime = nanoTime();
    }

    @Override
    public void postWrite(Object context, String methodName, Object[] args)
    {
        long now = nanoTime();
        PerCallMethodStats ctx = (PerCallMethodStats)context;
        ctx.postWriteTime = now;
        stats.get(methodName).addWriteTime(nanosBetween(ctx.preWriteTime, now));
    }

    @Override
    public void preRead(Object context, String methodName)
    {
        long now = nanoTime();
        PerCallMethodStats ctx = (PerCallMethodStats)context;
        ctx.preReadTime = now;
        stats.get(methodName).addInvokeTime(nanosBetween(ctx.postWriteTime, now));
    }

    @Override
    public void preReadException(Object context, String methodName, Throwable t)
    {
        preRead(context, methodName);
        ((PerCallMethodStats)context).success = false;
    }

    @Override
    public void postRead(Object context, String methodName, Object result)
    {
        stats.get(methodName).addReadTime(nanosSince(((PerCallMethodStats) context).preReadTime));
    }

    @Override
    public void postReadException(Object context, String methodName, Throwable t)
    {
        postRead(context, methodName, null);
        ((PerCallMethodStats)context).success = false;
    }

    @Override
    public void done(Object context, String methodName)
    {
        PerCallMethodStats ctx = (PerCallMethodStats)context;
        Duration duration = nanosSince(ctx.startTime);
        if (ctx.success) {
            stats.get(methodName).addSuccessTime(duration);
        } else {
            stats.get(methodName).addErrorTime(duration);
        }
    }
}
