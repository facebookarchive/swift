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
public final class SingleQueryInfo
{
    private final String logicalTableName;
    private final Map<String, String> filter;
    private final List<String> selectList;

    @ThriftConstructor
    public SingleQueryInfo(String logicalTableName, Map<String, String> filter, List<String> selectList)
    {
        this.logicalTableName = logicalTableName;
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
    public String getLogicalTableName()
    {
        return logicalTableName;
    }

    @ThriftField(2)
    public Map<String, String> getFilter()
    {
        return filter;
    }

    @ThriftField(3)
    public List<String> getSelectList()
    {
        return selectList;
    }
}
