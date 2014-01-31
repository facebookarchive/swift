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
package com.facebook.swift.generator;

public class SwiftJavaType
{
    private final String thriftNamespace;
    private final String javaTypeName;
    private final String thriftTypeName;
    private final String javaPackageName;

    public SwiftJavaType(
            final String thriftNamespace,
            final String javaTypeName,
            final String thriftTypeName,
            final String javaPackageName)
    {
        this.thriftNamespace = thriftNamespace;
        this.javaTypeName = javaTypeName;
        this.thriftTypeName = thriftTypeName;
        this.javaPackageName = javaPackageName;
    }

    public String getPackage()
    {
        return javaPackageName;
    }

    public String getSimpleName()
    {
        return javaTypeName;
    }

    public String getClassName()
    {
        return javaPackageName + "." + javaTypeName;
    }

    public String getKey()
    {
        return thriftNamespace + "." + thriftTypeName;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((javaPackageName == null) ? 0 : javaPackageName.hashCode());
        result = prime * result + ((javaTypeName == null) ? 0 : javaTypeName.hashCode());
        result = prime * result + ((thriftNamespace == null) ? 0 : thriftNamespace.hashCode());
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
        final SwiftJavaType other = (SwiftJavaType) obj;
        if (javaPackageName == null) {
            if (other.javaPackageName != null) {
                return false;
            }
        }
        else if (!javaPackageName.equals(other.javaPackageName)) {
            return false;
        }
        if (javaTypeName == null) {
            if (other.javaTypeName != null) {
                return false;
            }
        }
        else if (!javaTypeName.equals(other.javaTypeName)) {
            return false;
        }
        if (thriftNamespace == null) {
            if (other.thriftNamespace != null) {
                return false;
            }
        }
        else if (!thriftNamespace.equals(other.thriftNamespace)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "[thrift namespace=" + thriftNamespace +
               ", javaTypeName=" + javaTypeName +
               ", thriftTypeName=" + thriftTypeName +
               ", java package=" + javaPackageName + "]";
    }
}
