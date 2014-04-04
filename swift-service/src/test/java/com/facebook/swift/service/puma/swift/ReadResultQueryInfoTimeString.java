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
public final class ReadResultQueryInfoTimeString
{
    private final String startTimeResultWindow;
    private final Map<String, String> columnNameValueMap;

    @ThriftConstructor
    public ReadResultQueryInfoTimeString(String startTimeResultWindow, Map<String, String> columnNameValueMap)
    {
        this.startTimeResultWindow = startTimeResultWindow;
        if (columnNameValueMap != null) {
            this.columnNameValueMap = ImmutableMap.copyOf(columnNameValueMap);
        }
        else {
            this.columnNameValueMap = ImmutableMap.of();
        }
    }

    @ThriftField(1)
    public String getStartTimeResultWindow()
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

        final ReadResultQueryInfoTimeString that = (ReadResultQueryInfoTimeString) o;

        if (!columnNameValueMap.equals(that.columnNameValueMap)) {
            return false;
        }
        if (startTimeResultWindow != null ? !startTimeResultWindow.equals(that.startTimeResultWindow) : that.startTimeResultWindow != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = startTimeResultWindow != null ? startTimeResultWindow.hashCode() : 0;
        result = 31 * result + columnNameValueMap.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ReadResultQueryInfoTimeString");
        sb.append("{startTimeResultWindow='").append(startTimeResultWindow).append('\'');
        sb.append(", columnNameValueMap=").append(columnNameValueMap);
        sb.append('}');
        return sb.toString();
    }
}
