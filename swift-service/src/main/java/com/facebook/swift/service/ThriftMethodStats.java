/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

import io.airlift.stats.TimedStat;
import io.airlift.units.Duration;
import org.weakref.jmx.Managed;
import org.weakref.jmx.Nested;

public class ThriftMethodStats
{
    private final TimedStat success = new TimedStat();
    private final TimedStat read = new TimedStat();
    private final TimedStat invoke = new TimedStat();
    private final TimedStat write = new TimedStat();
    private final TimedStat error = new TimedStat();

    @Managed
    @Nested
    public TimedStat getRead()
    {
        return read;
    }

    @Managed
    @Nested
    public TimedStat getInvoke()
    {
        return invoke;
    }

    @Managed
    @Nested
    public TimedStat getWrite()
    {
        return write;
    }

    @Managed
    @Nested
    public TimedStat getSuccess()
    {
        return success;
    }

    @Managed
    @Nested
    public TimedStat getError()
    {
        return error;
    }

    public void addReadTime(Duration duration)
    {
        read.addValue(duration);
    }

    public void addInvokeTime(Duration duration)
    {
        invoke.addValue(duration);
    }

    public void addWriteTime(Duration duration)
    {
        write.addValue(duration);
    }

    public void addSuccessTime(Duration duration)
    {
        success.addValue(duration);
    }

    public void addErrorTime(Duration duration)
    {
        error.addValue(duration);
    }
}
