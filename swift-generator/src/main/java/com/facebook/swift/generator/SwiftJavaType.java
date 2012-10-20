package com.facebook.swift.generator;

public class SwiftJavaType
{
    private final String thriftNamespace;
    private final String name;
    private final String javaPackageName;

    public SwiftJavaType(final String thriftNamespace,
                         final String name,
                         final String javaPackageName)
    {
        this.thriftNamespace = thriftNamespace;
        this.name = name;
        this.javaPackageName = javaPackageName;
    }

    public String getPackage()
    {
        return javaPackageName;
    }

    public String getSimpleName()
    {
        return name;
    }

    public String getClassName()
    {
        return javaPackageName + "." + name;
    }

    public String getKey()
    {
        return thriftNamespace + "." + name;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((javaPackageName == null) ? 0 : javaPackageName.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((thriftNamespace == null) ? 0 : thriftNamespace.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SwiftJavaType other = (SwiftJavaType) obj;
        if (javaPackageName == null) {
            if (other.javaPackageName != null)
                return false;
        }
        else if (!javaPackageName.equals(other.javaPackageName))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        if (thriftNamespace == null) {
            if (other.thriftNamespace != null)
                return false;
        }
        else if (!thriftNamespace.equals(other.thriftNamespace))
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "SwiftJavaType [thriftNamespace=" + thriftNamespace + ", name=" + name + ", javaPackageName=" + javaPackageName + "]";
    }
}



