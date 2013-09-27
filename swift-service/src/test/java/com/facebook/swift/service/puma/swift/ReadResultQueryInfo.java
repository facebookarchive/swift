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
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@ThriftStruct
public final class ReadResultQueryInfo
{
    private final long startTimeResultWindow;
    private final Map<String, String> columnNameValueMap;

    @ThriftConstructor
    public ReadResultQueryInfo(long startTimeResultWindow, Map<String, String> columnNameValueMap)
    {
        this.startTimeResultWindow = startTimeResultWindow;
        if (columnNameValueMap != null) {
            this.columnNameValueMap = ImmutableMap.copyOf(columnNameValueMap);
        }
        else {
            this.columnNameValueMap = null;
        }
    }

    @ThriftField(1)
    public long getStartTimeResultWindow()
    {
        return startTimeResultWindow;
    }

    @ThriftField(2)
    public Map<String, String> getColumnNameValueMap()
    {
        return columnNameValueMap;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ReadResultQueryInfo that = (ReadResultQueryInfo) o;

        if (startTimeResultWindow != that.startTimeResultWindow) {
            return false;
        }
        if (!columnNameValueMap.equals(that.columnNameValueMap)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = (int) (startTimeResultWindow ^ (startTimeResultWindow >>> 32));
        result = 31 * result + columnNameValueMap.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ReadResultQueryInfo");
        sb.append("{startTimeResultWindow=").append(startTimeResultWindow);
        sb.append(", columnNameValueMap=").append(columnNameValueMap);
        sb.append('}');
        return sb.toString();
    }
}
