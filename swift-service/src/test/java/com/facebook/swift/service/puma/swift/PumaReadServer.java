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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

public class PumaReadServer implements PumaReadService
{
    private ReadSemanticException exception;

    public void setException(ReadSemanticException exception)
    {
        this.exception = exception;
    }

    @Override
    public List<ReadResultQueryInfo> getResult(List<ReadQueryInfoTimeString> reader)
            throws ReadSemanticException
    {
        if (exception != null) {
            throw exception;
        }

        ImmutableList.Builder<ReadResultQueryInfo> result = ImmutableList.builder();
        for (ReadQueryInfoTimeString readQueryInfoTimeString : reader) {
            result.add(new ReadResultQueryInfo(1, data(readQueryInfoTimeString.getSelectList())));
        }
        return result.build();
    }

    @Override
    public List<ReadResultQueryInfoTimeString> getResultTimeString(List<ReadQueryInfoTimeString> reader)
            throws ReadSemanticException
    {
        if (exception != null) {
            throw exception;
        }

        ImmutableList.Builder<ReadResultQueryInfoTimeString> result = ImmutableList.builder();
        for (ReadQueryInfoTimeString readQueryInfoTimeString : reader) {
            result.add(new ReadResultQueryInfoTimeString(
                    readQueryInfoTimeString.getStartTime(),
                    data(readQueryInfoTimeString.getSelectList())));
        }
        return result.build();
    }

    @Override
    public List<ReadResultQueryInfo> mergeQueryAggregation(MergeAggregationQueryInfo mergeAggregationQueryInfo)
            throws ReadSemanticException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long latestQueryableTime(String category, String appName, List<Integer> bucketNumbers)
            throws ReadSemanticException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> latestQueryableTimes(String category, String appName, List<Integer> bucketNumbers)
            throws ReadSemanticException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close()
    {
    }

    private Map<String, String> data(List<String> columns)
    {
        ImmutableMap.Builder<String, String> data = ImmutableMap.builder();
        for (String column : columns) {
            data.put(column, column);
        }
        return data.build();
    }
}
