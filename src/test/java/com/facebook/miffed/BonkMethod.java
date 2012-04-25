/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed;

@ThriftStruct(name = "Bonk")
public class BonkMethod
{
    private String message;
    private int type;

    @ThriftField(id = 1)
    public String getMessage()
    {
        return message;
    }

    @ThriftField(id = 2)
    public int getType()
    {
        return type;
    }

    @ThriftField
    public void setData(
            @ThriftField(name = "message") String message,
            @ThriftField(name = "type") int type)
    {
        this.message = message;
        this.type = type;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BonkMethod bonkBean = (BonkMethod) o;

        if (type != bonkBean.type) {
            return false;
        }
        if (message != null ? !message.equals(bonkBean.message) : bonkBean.message != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + type;
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("BonkMethod");
        sb.append("{message='").append(message).append('\'');
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
