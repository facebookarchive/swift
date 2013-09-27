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
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

@ThriftStruct
public final class ReadQueryInfoTimeString
{
    private final String name;
    private final String startTime;
    private final String endTime;
    private final int numSteps;
    private final Map<String, String> filter;
    private final List<String> selectList;

    @ThriftConstructor
    public ReadQueryInfoTimeString(
            String name,
            String startTime,
            String endTime,
            int numSteps,
            Map<String, String> filter,
            List<String> selectList)
    {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numSteps = numSteps;
        if (filter != null) {
            this.filter = ImmutableMap.copyOf(filter);
        }
        else {
            this.filter = ImmutableMap.of();
        }
        if (selectList != null) {
            this.selectList = ImmutableList.copyOf(selectList);
        }
        else {
            this.selectList = ImmutableList.of();
        }
    }

    @ThriftField(1)
    public String getName()
    {
        return name;
    }

    @ThriftField(2)
    public String getStartTime()
    {
        return startTime;
    }

    @ThriftField(3)
    public String getEndTime()
    {
        return endTime;
    }

    @ThriftField(4)
    public int getNumSteps()
    {
        return numSteps;
    }

    @ThriftField(5)
    public Map<String, String> getFilter()
    {
        return filter;
    }

    @ThriftField(6)
    public List<String> getSelectList()
    {
        return selectList;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ReadQueryInfoTimeString");
        sb.append("{name='").append(name).append('\'');
        sb.append(", startTime='").append(startTime).append('\'');
        sb.append(", endTime='").append(endTime).append('\'');
        sb.append(", numSteps=").append(numSteps);
        sb.append(", filter=").append(filter);
        sb.append(", selectList=").append(selectList);
        sb.append('}');
        return sb.toString();
    }
}
