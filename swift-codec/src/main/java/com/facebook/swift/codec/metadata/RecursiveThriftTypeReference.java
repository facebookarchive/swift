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
package com.facebook.swift.codec.metadata;

import com.facebook.swift.codec.ThriftProtocolType;

import java.lang.reflect.Type;
import java.util.Objects;

public class RecursiveThriftTypeReference implements ThriftTypeReference
{
    private final ThriftCatalog catalog;
    private final Type javaType;
    private final ThriftProtocolType protocolType;

    public RecursiveThriftTypeReference(ThriftCatalog catalog, Type javaType)
    {
        this.catalog = catalog;
        this.javaType = javaType;
        this.protocolType = catalog.getThriftProtocolType(javaType);
    }

    @Override
    public Type getJavaType()
    {
        return javaType;
    }

    @Override
    public ThriftProtocolType getProtocolType()
    {
        return protocolType;
    }

    @Override
    public boolean isRecursive()
    {
        return true;
    }

    @Override
    public ThriftType get()
    {
        ThriftType resolvedType = catalog.getThriftTypeFromCache(javaType);
        if (resolvedType == null) {
            throw new UnsupportedOperationException(
                String.format(
                    "Attempted to resolve a recursive reference to type '%s' before the " +
                    "referenced type was cached (most likely a recursive type support bug)",
                    javaType.getTypeName()));
        }
        return resolvedType;
    }

    @Override
    public String toString()
    {
        if (isResolved()) {
            return "Resolved reference to " + get();
        }
        else {
            return "Unresolved reference to ThriftType for " + javaType;
        }
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(catalog, javaType);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != RecursiveThriftTypeReference.class) {
            return false;
        }

        RecursiveThriftTypeReference that = (RecursiveThriftTypeReference) obj;

        return Objects.equals(this.catalog, that.catalog) &&
               Objects.equals(this.javaType, that.javaType);
    }

    private boolean isResolved()
    {
        return catalog.getThriftTypeFromCache(javaType) != null;
    }
}
