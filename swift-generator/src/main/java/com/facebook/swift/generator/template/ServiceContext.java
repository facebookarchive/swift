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

import com.google.common.collect.Lists;

import com.facebook.swift.generator.SwiftDocumentContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ServiceContext extends BaseJavaContext
{
    private final String name;
    private final String javaPackage;
    private final String javaName;
    private final Set<String> javaParents;
    private final Set<String> javaAsyncParents;

    private final List<MethodContext> methods = Lists.newArrayList();

    ServiceContext(SwiftDocumentContext swiftDocumentContext, String name, String javaPackage, String javaName, Set<String> javaParents)
    {
        super(swiftDocumentContext);
        this.name = name;
        this.javaPackage = javaPackage;
        this.javaName = javaName;
        this.javaParents = javaParents;
        Set<String> asyncParents = new HashSet<>();
        for (String parent: javaParents) {
            if(parent.equals("Closeable")) {
                asyncParents.add(parent);
            } else {
                asyncParents.add(parent + ".Async");
            }
        }
        this.javaAsyncParents = asyncParents;
    }

    public void addMethod(final MethodContext method)
    {
        this.methods.add(method);
    }

    public List<MethodContext> getMethods()
    {
        return methods;
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

    public Set<String> getJavaParents()
    {
        return javaParents;
    }

    public Set<String> getJavaAsyncParents() { return javaAsyncParents; }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((javaName == null) ? 0 : javaName.hashCode());
        result = prime * result + ((javaPackage == null) ? 0 : javaPackage.hashCode());
        result = prime * result + ((javaParents == null) ? 0 : javaParents.hashCode());
        result = prime * result + ((methods == null) ? 0 : methods.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
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
        ServiceContext other = (ServiceContext) obj;
        if (javaName == null) {
            if (other.javaName != null) {
                return false;
            }
        }
        else if (!javaName.equals(other.javaName)) {
            return false;
        }
        if (javaPackage == null) {
            if (other.javaPackage != null) {
                return false;
            }
        }
        else if (!javaPackage.equals(other.javaPackage)) {
            return false;
        }
        if (javaParents == null) {
            if (other.javaParents != null) {
                return false;
            }
        }
        else if (!javaParents.equals(other.javaParents)) {
            return false;
        }
        if (methods == null) {
            if (other.methods != null) {
                return false;
            }
        }
        else if (!methods.equals(other.methods)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "ServiceContext [name=" + name + ", javaPackage=" + javaPackage + ", javaName=" + javaName + ", javaParents=" + javaParents + ", methods=" + methods + "]";
    }
}
