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
package com.facebook.swift.parser.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MapType
        extends ContainerType
{
    private final ThriftType keyType;
    private final ThriftType valueType;

    public MapType(ThriftType keyType, ThriftType valueType, String cppType, List<TypeAnnotation> annotations)
    {
        super(cppType, annotations);
        this.keyType = checkNotNull(keyType, "keyType");
        this.valueType = checkNotNull(valueType, "valueType");
    }

    public ThriftType getKeyType()
    {
        return keyType;
    }

    public ThriftType getValueType()
    {
        return valueType;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("keyType", keyType)
                          .add("valueType", valueType)
                          .add("cppType", cppType)
                          .add("annotations", annotations)
                          .toString();
    }
}
