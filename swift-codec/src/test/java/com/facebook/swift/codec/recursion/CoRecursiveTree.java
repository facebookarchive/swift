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
package com.facebook.swift.codec.recursion;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;
import com.google.common.base.Objects;

import java.util.List;

@ThriftStruct
public class CoRecursiveTree
{
    @ThriftField(1)
    public List<CoRecursiveTreeHelper> children;

    @ThriftField(2)
    public String data;

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CoRecursiveTree that = (CoRecursiveTree) o;
        return Objects.equal(children, that.children) &&
               Objects.equal(data, that.data);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(children, data);
    }
}
