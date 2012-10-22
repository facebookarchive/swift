package com.facebook.swift.generator.template;

public class ExceptionContext
{
    private final String type;
    private final short id;

    ExceptionContext(String type, short id)
    {
        this.type = type;
        this.id = id;
    }

    public String getType()
    {
        return type;
    }

    public short getId()
    {
        return id;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        ExceptionContext other = (ExceptionContext) obj;
        if (id != other.id) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        }
        else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "ExceptionContext [type=" + type + ", id=" + id + "]";
    }


}
