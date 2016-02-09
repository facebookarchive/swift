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
package com.facebook.swift.generator.template;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class ConstantContext
    implements Comparable<ConstantContext>
{
    private final String name;
    private final String javaType;
    private final String javaName;
    private final String javaValue;

    ConstantContext(String name,
                 String javaType,
                 String javaName,
                 String javaValue)
    {
        this.name = name;
        this.javaType = javaType;
        this.javaName = javaName;
        this.javaValue = javaValue;
    }

    public String getName()
    {
        return name;
    }

    public String getJavaType()
    {
        return javaType;
    }

    public String getJavaName()
    {
        return javaName;
    }

    public String getJavaValue()
    {
        return javaValue;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(javaName, javaValue, javaType, name);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConstantContext that = (ConstantContext) obj;

        return Objects.equal(this.javaName, that.javaName)
            && Objects.equal(this.javaValue, that.javaValue)
            && Objects.equal(this.javaType, that.javaType)
            && Objects.equal(this.name, that.name);
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("name", name)
                          .add("javaType", javaType)
                          .add("javaName", javaName)
                          .add("javaValue", javaValue)
                          .toString();
    }

    @Override
    public int compareTo(ConstantContext that)
    {
        return this.name.compareTo(that.name);
    }
}
