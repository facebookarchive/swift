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
package com.facebook.swift.service.puma.swift;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;
import com.google.common.collect.ImmutableList;

import java.util.List;

@ThriftStruct
public final class MergeAggregationQueryInfo
{
    private final List<SingleQueryInfo> queries;
    private final long startTime;
    private final long endTime;
    private final int numSteps;

    @ThriftConstructor
    public MergeAggregationQueryInfo(List<SingleQueryInfo> queries, long startTime, long endTime, int numSteps)
    {
        if (queries != null) {
            this.queries = ImmutableList.copyOf(queries);
        }
        else {
            this.queries = ImmutableList.of();
        }
        this.startTime = startTime;
        this.endTime = endTime;
        this.numSteps = numSteps;
    }

    @ThriftField(1)
    public List<SingleQueryInfo> getQueries()
    {
        return queries;
    }

    @ThriftField(2)
    public long getStartTime()
    {
        return startTime;
    }

    @ThriftField(3)
    public long getEndTime()
    {
        return endTime;
    }

    @ThriftField(4)
    public int getNumSteps()
    {
        return numSteps;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("MergeAggregationQueryInfo");
        sb.append("{queries=").append(queries);
        sb.append(", startTime=").append(startTime);
        sb.append(", endTime=").append(endTime);
        sb.append(", numSteps=").append(numSteps);
        sb.append('}');
        return sb.toString();
    }
}
