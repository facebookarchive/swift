package com.facebook.swift.generator.template;

import com.google.common.collect.Lists;

import java.util.List;

public class StructContext
{
    private final String name;
    private final String namespace;
    private final String javaName;

    private final List<FieldContext> fields = Lists.newArrayList();

    StructContext(String name, String namespace, String javaName)
    {
        this.name = name;
        this.namespace = namespace;
        this.javaName = javaName;
    }

    public void addField(final FieldContext field)
    {
        this.fields.add(field);
    }

    public List<FieldContext> getFields()
    {
        return fields;
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

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + ((javaName == null) ? 0 : javaName.hashCode());
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
        StructContext other = (StructContext) obj;
        if (fields == null) {
            if (other.fields != null) {
                return false;
            }
        }
        else if (!fields.equals(other.fields)) {
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
        return "StructContext [name=" + name + ", namespace=" + namespace + ", javaName=" + javaName + ", fields=" + fields + "]";
    }
}
