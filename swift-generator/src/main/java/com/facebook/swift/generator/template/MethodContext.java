package com.facebook.swift.generator.template;

import com.google.common.collect.Lists;

import java.util.List;

public class MethodContext
{
    private final String name;
    private final boolean oneway;
    private final String javaName;
    private final String javaType;

    private final List<FieldContext> parameters = Lists.newArrayList();
    private final List<ExceptionContext> exceptions = Lists.newArrayList();

    MethodContext(String name, boolean oneway, String javaName, String javaType)
    {
        this.name = name;
        this.oneway = oneway;
        this.javaName = javaName;
        this.javaType = javaType;
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
