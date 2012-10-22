package com.facebook.swift.generator.template;

import com.google.common.collect.Lists;

import java.util.List;

public class EnumContext
{
    private final String namespace;
    private final String javaName;

    private final List<EnumFieldContext> fields = Lists.newArrayList();

    EnumContext(String namespace, String javaName)
    {
        this.namespace = namespace;
        this.javaName = javaName;
    }

    public void addField(final EnumFieldContext parameter)
    {
        this.fields.add(parameter);
    }

    public List<EnumFieldContext> getFields()
    {
        return fields;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public String getJavaName()
    {
        return javaName;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + ((javaName == null) ? 0 : javaName.hashCode());
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
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
        EnumContext other = (EnumContext) obj;
        if (fields == null) {
            if (other.fields != null)
                return false;
        }
        else if (!fields.equals(other.fields))
            return false;
        if (javaName == null) {
            if (other.javaName != null)
                return false;
        }
        else if (!javaName.equals(other.javaName))
            return false;
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        }
        else if (!namespace.equals(other.namespace))
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "EnumContext [namespace=" + namespace + ", javaName=" + javaName + ", fields=" + fields + "]";
    }
}
