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

import com.facebook.nifty.core.NiftyRequestContext;
import com.facebook.nifty.core.RequestContext;
import io.airlift.units.Duration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static io.airlift.units.Duration.nanosSince;
import static java.lang.System.nanoTime;

public class ThriftServiceStatsHandler extends ThriftEventHandler
{
    private final ConcurrentHashMap<String, ThriftMethodStats> stats = new ConcurrentHashMap<>();

    private static class PerCallMethodStats
    {
        public final RequestContext requestContext;
        public boolean success = true;
        public long startTime = nanoTime();
        public long preReadTime;
        public long postReadTime;
        public long preWriteTime;

        public PerCallMethodStats(RequestContext requestContext)
        {
            this.requestContext = requestContext;
        }
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
    public Object getContext(String methodName, RequestContext requestContext)
    {
        stats.putIfAbsent(methodName, new ThriftMethodStats());
        return new PerCallMethodStats(requestContext);
    }

    @Override
    public void preRead(Object context, String methodName)
    {
        ((PerCallMethodStats)context).preReadTime = nanoTime();
    }

    @Override
    public void postRead(Object context, String methodName, Object[] args)
    {
        long now = nanoTime();
        PerCallMethodStats ctx = (PerCallMethodStats)context;
        ctx.postReadTime = now;
        stats.get(methodName).addReadTime(nanosBetween(ctx.preReadTime, now));
        stats.get(methodName).addReadByteCount(getBytesRead(ctx));
    }

    @Override
    public void preWrite(Object context, String methodName, Object result)
    {
        long now = nanoTime();
        PerCallMethodStats ctx = (PerCallMethodStats)context;
        ctx.preWriteTime = now;
        stats.get(methodName).addInvokeTime(nanosBetween(ctx.postReadTime, now));
    }

    @Override
    public void preWriteException(Object context, String methodName, Throwable t)
    {
        preWrite(context, methodName, null);
        ((PerCallMethodStats)context).success = false;
    }

    @Override
    public void postWrite(Object context, String methodName, Object result)
    {
        PerCallMethodStats ctx = (PerCallMethodStats) context;
        stats.get(methodName).addWriteTime(nanosSince(ctx.preWriteTime));
        stats.get(methodName).addWriteByteCount(getBytesWritten(ctx));
    }

    @Override
    public void postWriteException(Object context, String methodName, Throwable t)
    {
        postWrite(context, methodName, null);
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

    private int getBytesRead(PerCallMethodStats ctx)
    {
        if (!(ctx.requestContext instanceof NiftyRequestContext)) {
            return 0;
        }

        NiftyRequestContext requestContext = (NiftyRequestContext) ctx.requestContext;
        return requestContext.getNiftyTransport().getReadByteCount();
    }

    private int getBytesWritten(PerCallMethodStats ctx)
    {
        if (!(ctx.requestContext instanceof NiftyRequestContext)) {
            // Standard TTransport interface doesn't give us a way to determine how many bytes
            // were read
            return 0;
        }

        NiftyRequestContext requestContext = (NiftyRequestContext) ctx.requestContext;
        return requestContext.getNiftyTransport().getWrittenByteCount();
    }
}
