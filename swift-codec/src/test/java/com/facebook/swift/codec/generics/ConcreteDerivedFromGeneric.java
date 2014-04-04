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
package com.facebook.swift.codec.generics;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import java.util.Objects;

@ThriftStruct
public final class ConcreteDerivedFromGeneric extends GenericThriftStructBase<Double>
{
    private final Double concreteProperty;

    @ThriftConstructor
    public ConcreteDerivedFromGeneric(
            @ThriftField(1) Double genericProperty,
            @ThriftField(2) Double concreteProperty)
    {
        super(genericProperty);
        this.concreteProperty = concreteProperty;
    }

    @ThriftField(2)
    public Double getConcreteProperty()
    {
        return concreteProperty;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ConcreteDerivedFromGeneric other = (ConcreteDerivedFromGeneric) obj;
        return Objects.equals(this.concreteProperty, other.concreteProperty) && super.equals(obj);
    }
}
