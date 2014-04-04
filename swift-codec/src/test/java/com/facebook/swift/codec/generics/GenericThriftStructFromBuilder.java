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

@ThriftStruct(builder = GenericThriftStructFromBuilder.Builder.class)
public final class GenericThriftStructFromBuilder<S, T>
{
    private final S firstGenericProperty;
    private final T secondGenericProperty;

    private GenericThriftStructFromBuilder(S firstGenericProperty, T secondGenericProperty)
    {
        this.firstGenericProperty = firstGenericProperty;
        this.secondGenericProperty = secondGenericProperty;
    }

    @ThriftField(1)
    public S getFirstGenericProperty()
    {
        return firstGenericProperty;
    }

    @ThriftField(2)
    public T getSecondGenericProperty()
    {
        return secondGenericProperty;
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
        GenericThriftStructFromBuilder<?, ?> other = (GenericThriftStructFromBuilder<?, ?>) obj;
        return
                Objects.equals(firstGenericProperty, other.firstGenericProperty) &&
                Objects.equals(secondGenericProperty, other.secondGenericProperty);
    }

    public static class Builder<X, Y>
    {
        private X firstGenericProperty;
        private Y secondGenericProperty;

        @ThriftField(1)
        public Builder<X, Y> setFirstGenericProperty(X firstGenericProperty)
        {
            this.firstGenericProperty = firstGenericProperty;
            return this;
        }

        @ThriftField(2)
        public Builder<X, Y> setSecondGenericProperty(Y secondGenericProperty)
        {
            this.secondGenericProperty = secondGenericProperty;
            return this;
        }

        @ThriftConstructor
        public GenericThriftStructFromBuilder<X, Y> build()
        {
            return new GenericThriftStructFromBuilder<>(firstGenericProperty, secondGenericProperty);
        }
    }
}
