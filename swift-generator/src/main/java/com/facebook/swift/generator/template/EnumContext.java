package com.facebook.swift.generator.template;

import java.util.List;

import com.google.common.collect.Lists;

public class EnumContext implements JavaContext
{
    private final String javaPackage;
    private final String javaName;

    private final List<EnumFieldContext> fields = Lists.newArrayList();

    EnumContext(String javaPackage, String javaName)
    {
        this.javaPackage = javaPackage;
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + ((javaName == null) ? 0 : javaName.hashCode());
        result = prime * result + ((javaPackage == null) ? 0 : javaPackage.hashCode());
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
        if (javaPackage == null) {
            if (other.javaPackage != null)
                return false;
        }
        else if (!javaPackage.equals(other.javaPackage))
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "EnumContext [javaPackage=" + javaPackage + ", javaName=" + javaName + ", fields=" + fields + "]";
    }
}
