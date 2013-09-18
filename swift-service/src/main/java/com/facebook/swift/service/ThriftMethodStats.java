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
    private final DistributionStat readSize = new DistributionStat();
    private final DistributionStat writeSize = new DistributionStat();
    private final CounterStat readSizeTotal = new CounterStat();
    private final CounterStat writeSizeTotal = new CounterStat();

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
