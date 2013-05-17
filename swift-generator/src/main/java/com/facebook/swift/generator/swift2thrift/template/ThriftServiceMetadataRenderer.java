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

import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import org.stringtemplate.v4.AttributeRenderer;

import java.util.Locale;
import java.util.Map;

public class ThriftServiceMetadataRenderer implements AttributeRenderer
{
    private final Map<ThriftServiceMetadata, String> serviceMap;

    public ThriftServiceMetadataRenderer(Map<ThriftServiceMetadata, String> serviceMap)
    {
        this.serviceMap = serviceMap;
    }

    @Override
    public String toString(Object o, String formatString, Locale locale)
    {
        return toString((ThriftServiceMetadata)o);
    }

    public String toString(ThriftServiceMetadata s) {
        String result = serviceMap.get(s);
        return result == null ? s.getName() : result + "." + s.getName();
    }
}
