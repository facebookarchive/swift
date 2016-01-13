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

import com.facebook.swift.codec.metadata.ThriftType;
import org.stringtemplate.v4.AttributeRenderer;

import java.util.Locale;
import java.util.Map;

public class ThriftTypeRenderer implements AttributeRenderer
{
    private final Map<ThriftType, String> typenameMap;

    public ThriftTypeRenderer(Map<ThriftType, String> typenameMap)
    {
        this.typenameMap = typenameMap;
    }

    @Override
    public String toString(Object o, String format, Locale locale)
    {
        return toString((ThriftType)o);
    }

    public String toString(ThriftType t)
    {
        switch (t.getProtocolType()) {
            case BOOL:      return "bool";
            case BYTE:      return "byte";
            case DOUBLE:    return "double";
            case I16:       return "i16";
            case I32:       return "i32";
            case I64:       return "i64";
            case ENUM:      return prefix(t) + t.getEnumMetadata().getEnumName();
            case MAP:       return "map<" + toString(t.getKeyTypeReference().get()) + ", " + toString(t.getValueTypeReference().get()) + ">";
            case SET:       return "set<" + toString(t.getValueTypeReference().get()) + ">";
            case LIST:      return "list<" + toString(t.getValueTypeReference().get()) + ">";
            // void is encoded as a struct
            case STRUCT:    return t.equals(ThriftType.VOID) ? "void" : prefix(t) + t.getStructMetadata().getStructName();
            case STRING:    return "string";
            case BINARY:    return "binary";
        }
        throw new IllegalStateException("Bad protocol type" + t.getProtocolType());
    }

    private String prefix(ThriftType t)
    {
        String result = typenameMap.get(t);
        return result == null ? "" : result + ".";
    }
}
