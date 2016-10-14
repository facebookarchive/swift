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

import com.facebook.swift.generator.SwiftDocumentContext;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class ConstantsContext extends BaseJavaContext
{
    private final String name;
    private final String javaPackage;
    private final String javaName;

    private final SortedSet<ConstantContext> constants = new TreeSet<>();

    ConstantsContext(final SwiftDocumentContext swiftDocumentContext, final String name, final String javaPackage, final String javaName)
    {
        super(swiftDocumentContext);
        this.name = name;
        this.javaPackage = javaPackage;
        this.javaName = javaName;
    }

    public void addConstant(final ConstantContext field)
    {
        this.constants.add(field);
    }

    public Set<ConstantContext> getConstants()
    {
        return constants;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String getJavaPackage()
    {
        return javaPackage;
    }

    @Override
    public String getJavaName()
    {
        return javaName;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(javaName, javaPackage, name);
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
        ConstantsContext that = (ConstantsContext) obj;

        return Objects.equal(this.javaName, that.javaName)
            && Objects.equal(this.javaPackage, that.javaPackage)
            && Objects.equal(this.name, that.name);
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("name", name)
                          .add("javaName", javaName)
                          .add("javaPackage", javaPackage)
                          .toString();
    }
}
