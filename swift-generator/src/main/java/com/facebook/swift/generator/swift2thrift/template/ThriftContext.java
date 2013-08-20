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
package com.facebook.swift.generator.swift2thrift.template;

import com.google.common.collect.Lists;
import com.facebook.swift.codec.metadata.ThriftEnumMetadata;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;

import java.util.List;
import java.util.Map;

public class ThriftContext
{
    private final String namespace;
    private final List<String> includes;
    private final List<ThriftStructMetadata> thriftStructs = Lists.newArrayList();
    private final List<ThriftEnumMetadata> thriftEnums = Lists.newArrayList();
    private final List<ThriftServiceMetadata> thriftServices;
    private final Map<String, String> customNamespaces;

    public ThriftContext(String namespace, List<String> includes, List<ThriftType> thriftTypes, List<ThriftServiceMetadata> thriftServices, Map<String, String> namespaceMap)
    {
        this.namespace = namespace;
        this.includes = includes;
        for (ThriftType t: thriftTypes) {
            switch (t.getProtocolType()) {
            case STRUCT:
                this.thriftStructs.add(t.getStructMetadata());
                break;
            case ENUM:
                this.thriftEnums.add(t.getEnumMetadata());
                break;
            default:
                throw new IllegalStateException("Unknown protocol type: " + t.getProtocolType());
            }
        }
        this.thriftServices = thriftServices;
        customNamespaces = namespaceMap;
    }

    public List<ThriftStructMetadata> getStructs()
    {
        return thriftStructs;
    }

    public List<ThriftEnumMetadata> getEnums()
    {
        return thriftEnums;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public Map<String, String> getCustomNamespaces()
    {
        return customNamespaces;
    }

    public List<ThriftServiceMetadata> getServices()
    {
        return thriftServices;
    }

    public List<String> getIncludes()
    {
        return includes;
    }
}
