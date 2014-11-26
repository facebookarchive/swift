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
package com.facebook.swift.service;

import io.airlift.stats.CounterStat;
import io.airlift.stats.DistributionStat;
import io.airlift.stats.TimeStat;
import io.airlift.units.Duration;
import org.weakref.jmx.Managed;
import org.weakref.jmx.Nested;

public class ThriftMethodStats
{
    private final TimeStat success = new TimeStat();
    private final TimeStat read = new TimeStat();
    private final TimeStat invoke = new TimeStat();
    private final TimeStat write = new TimeStat();
    private final TimeStat error = new TimeStat();
    private final DistributionStat readSize = new DistributionStat();
    private final DistributionStat writeSize = new DistributionStat();
    private final CounterStat readSizeTotal = new CounterStat();
    private final CounterStat writeSizeTotal = new CounterStat();

    @Managed
    @Nested
    public TimeStat getRead()
    {
        return read;
    }

    @Managed
    @Nested
    public TimeStat getInvoke()
    {
        return invoke;
    }

    @Managed
    @Nested
    public TimeStat getWrite()
    {
        return write;
    }

    @Managed
    @Nested
    public TimeStat getSuccess()
    {
        return success;
    }

    @Managed
    @Nested
    public TimeStat getError()
    {
        return error;
    }

    @Managed
    @Nested
    public DistributionStat getReadSize()
    {
        return readSize;
    }

    @Managed
    @Nested
    public DistributionStat getWriteSize()
    {
        return writeSize;
    }

    @Managed
    @Nested
    public CounterStat getReadSizeTotal()
    {
        return readSizeTotal;
    }

    @Managed
    @Nested
    public CounterStat getWriteSizeTotal()
    {
        return writeSizeTotal;
    }

    public void addReadTime(Duration duration)
    {
        read.add(duration);
    }

    public void addInvokeTime(Duration duration)
    {
        invoke.add(duration);
    }

    public void addWriteTime(Duration duration)
    {
        write.add(duration);
    }

    public void addSuccessTime(Duration duration)
    {
        success.add(duration);
    }

    public void addErrorTime(Duration duration)
    {
        error.add(duration);
    }

    public void addReadByteCount(int readByteCount)
    {
        readSizeTotal.update(readByteCount);
        readSize.add(readByteCount);
    }

    public void addWriteByteCount(int writeByteCount)
    {
        writeSizeTotal.update(writeByteCount);
        writeSize.add(writeByteCount);
    }
}
