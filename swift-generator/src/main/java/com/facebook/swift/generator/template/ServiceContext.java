package com.facebook.swift.generator.template;

import com.google.common.collect.Lists;

import java.util.List;


public class ServiceContext
{
    private final String name;
    private final String namespace;
    private final String javaName;
    private final String javaParent;

    private final List<MethodContext> methods = Lists.newArrayList();

    ServiceContext(String name, String namespace, String javaName, String javaParent)
    {
        this.name = name;
        this.namespace = namespace;
        this.javaName = javaName;
        this.javaParent = javaParent;
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

    public String getNamespace()
    {
        return namespace;
    }

    public String getJavaName()
    {
        return javaName;
    }

    public String getJavaParent()
    {
        return javaParent;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((javaName == null) ? 0 : javaName.hashCode());
        result = prime * result + ((javaParent == null) ? 0 : javaParent.hashCode());
        result = prime * result + ((methods == null) ? 0 : methods.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
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
        if (javaParent == null) {
            if (other.javaParent != null) {
                return false;
            }
        }
        else if (!javaParent.equals(other.javaParent)) {
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
        if (namespace == null) {
            if (other.namespace != null) {
                return false;
            }
        }
        else if (!namespace.equals(other.namespace)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "ServiceContext [name=" + name + ", namespace=" + namespace + ", javaName=" + javaName + ", javaParent=" + javaParent + ", methods=" + methods + "]";
    }
}
