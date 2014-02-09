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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Represents a method element of a service. If the name is null, this is a method that does nto represent
 * a Thrift IDL method and has no annotations (e.g. the {@link Closeable#close()} method.
 */
public class MethodContext
{
    private final String name;
    private final boolean oneway;
    private final String javaName;
    private final String javaType;
    private final String boxedJavaType;
    private final boolean allowAsync;

    private final List<FieldContext> parameters = Lists.newArrayList();
    private final List<ExceptionContext> exceptions = Lists.newArrayList();

    MethodContext(@Nullable String name, boolean oneway, String javaName, String javaType, String boxedJavaType)
    {
        this(name, oneway, javaName, javaType, boxedJavaType, true /* allow async */);
    }

    MethodContext(@Nullable String name, boolean oneway, String javaName, String javaType, String boxedJavaType, boolean allowAsync)
    {
        this.name = name;
        this.oneway = oneway;
        this.javaName = javaName;
        this.javaType = javaType;
        this.boxedJavaType = boxedJavaType;
        this.allowAsync = allowAsync;
    }

    public void addParameter(final FieldContext parameter)
    {
        this.parameters.add(parameter);
    }

    public void addException(final ExceptionContext exception)
    {
        this.exceptions.add(exception);
    }

    public List<FieldContext> getParameters()
    {
        return parameters;
    }

    public List<ExceptionContext> getExceptions()
    {
        return exceptions;
    }

    public Collection<ExceptionContext> getAnnotatedExceptions()
    {
        return Collections2.filter(exceptions, new Predicate<ExceptionContext>() {

            @Override
            public boolean apply(ExceptionContext exceptionContext)
            {
                return exceptionContext.getId() != null;
            }

        });
    }

    public String getName()
    {
        return name;
    }

    public boolean isOneway()
    {
        return oneway;
    }

    public String getJavaName()
    {
        return javaName;
    }

    public String getJavaType()
    {
        return javaType;
    }

    public String getBoxedJavaType() { return boxedJavaType; }

    public boolean getAllowAsync() { return allowAsync; }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((exceptions == null) ? 0 : exceptions.hashCode());
        result = prime * result + ((javaName == null) ? 0 : javaName.hashCode());
        result = prime * result + ((javaType == null) ? 0 : javaType.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (oneway ? 1231 : 1237);
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
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
        MethodContext other = (MethodContext) obj;
        if (exceptions == null) {
            if (other.exceptions != null) {
                return false;
            }
        }
        else if (!exceptions.equals(other.exceptions)) {
            return false;
        }
        if (javaName == null) {
            if (other.javaName != null) {
                return false;
            }
        }
        else if (!javaName.equals(other.javaName)) {
            return false;
        }
        if (javaType == null) {
            if (other.javaType != null) {
                return false;
            }
        }
        else if (!javaType.equals(other.javaType)) {
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
        if (oneway != other.oneway) {
            return false;
        }
        if (parameters == null) {
            if (other.parameters != null) {
                return false;
            }
        }
        else if (!parameters.equals(other.parameters)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "MethodContext [name=" + name + ", oneway=" + oneway + ", javaName=" + javaName + ", javaType=" + javaType + ", parameters=" + parameters + ", exceptions=" + exceptions + "]";
    }
}
