package com.facebook.swift.generator.template;

public class EnumFieldContext
{
    private final String javaName;
    private final Long value;

    EnumFieldContext(String javaName,
                     Long value)
    {
        this.javaName = javaName;
        this.value = value;
    }

    public String getJavaName()
    {
        return javaName;
    }

    public Long getValue()
    {
        return value;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((javaName == null) ? 0 : javaName.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        EnumFieldContext other = (EnumFieldContext) obj;
        if (javaName == null) {
            if (other.javaName != null)
                return false;
        }
        else if (!javaName.equals(other.javaName))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        }
        else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "EnumFieldContext [javaName=" + javaName + ", value=" + value + "]";
    }
}