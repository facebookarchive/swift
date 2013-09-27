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
package com.facebook.swift.codec;

import java.util.List;

@ThriftStruct
public final class CoercionBean
{
    private Boolean booleanValue;
    private Byte byteValue;
    private Short shortValue;
    private Integer integerValue;
    private Long longValue;
    private Float floatValue;
    private Double doubleValue;

    private float primitiveFloat;
    private List<Float> floatList;

    public CoercionBean()
    {
    }

    public CoercionBean(
            Boolean booleanValue,
            Byte byteValue,
            Short shortValue,
            Integer integerValue,
            Long longValue,
            Float floatValue,
            Double doubleValue,
            float primitiveFloat,
            List<Float> floatList
    )
    {
        this.booleanValue = booleanValue;
        this.byteValue = byteValue;
        this.shortValue = shortValue;
        this.integerValue = integerValue;
        this.longValue = longValue;
        this.floatValue = floatValue;
        this.doubleValue = doubleValue;
        this.primitiveFloat = primitiveFloat;
        this.floatList = floatList;
    }

    @ThriftField(1)
    public Boolean getBooleanValue()
    {
        return booleanValue;
    }

    @ThriftField
    public void setBooleanValue(Boolean booleanValue)
    {
        this.booleanValue = booleanValue;
    }

    @ThriftField(2)
    public Byte getByteValue()
    {
        return byteValue;
    }

    @ThriftField
    public void setByteValue(Byte byteValue)
    {
        this.byteValue = byteValue;
    }

    @ThriftField(3)
    public Short getShortValue()
    {
        return shortValue;
    }

    @ThriftField
    public void setShortValue(Short shortValue)
    {
        this.shortValue = shortValue;
    }

    @ThriftField(4)
    public Integer getIntegerValue()
    {
        return integerValue;
    }

    @ThriftField
    public void setIntegerValue(Integer integerValue)
    {
        this.integerValue = integerValue;
    }

    @ThriftField(5)
    public Long getLongValue()
    {
        return longValue;
    }

    @ThriftField
    public void setLongValue(Long longValue)
    {
        this.longValue = longValue;
    }

    @ThriftField(6)
    public Float getFloatValue()
    {
        return floatValue;
    }

    @ThriftField
    public void setFloatValue(Float floatValue)
    {
        this.floatValue = floatValue;
    }

    @ThriftField(7)
    public Double getDoubleValue()
    {
        return doubleValue;
    }

    @ThriftField
    public void setDoubleValue(Double doubleValue)
    {
        this.doubleValue = doubleValue;
    }

    @ThriftField(8)
    public float getPrimitiveFloat()
    {
        return primitiveFloat;
    }

    @ThriftField
    public void setPrimitiveFloat(float primitiveFloat)
    {
        this.primitiveFloat = primitiveFloat;
    }

    @ThriftField(9)
    public List<Float> getFloatList()
    {
        return floatList;
    }

    @ThriftField
    public void setFloatList(List<Float> floatList)
    {
        this.floatList = floatList;
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

        final CoercionBean that = (CoercionBean) o;

        if (Float.compare(that.primitiveFloat, primitiveFloat) != 0) {
            return false;
        }
        if (booleanValue != null ? !booleanValue.equals(that.booleanValue) : that.booleanValue != null) {
            return false;
        }
        if (byteValue != null ? !byteValue.equals(that.byteValue) : that.byteValue != null) {
            return false;
        }
        if (doubleValue != null ? !doubleValue.equals(that.doubleValue) : that.doubleValue != null) {
            return false;
        }
        if (floatList != null ? !floatList.equals(that.floatList) : that.floatList != null) {
            return false;
        }
        if (floatValue != null ? !floatValue.equals(that.floatValue) : that.floatValue != null) {
            return false;
        }
        if (integerValue != null ? !integerValue.equals(that.integerValue) : that.integerValue != null) {
            return false;
        }
        if (longValue != null ? !longValue.equals(that.longValue) : that.longValue != null) {
            return false;
        }
        if (shortValue != null ? !shortValue.equals(that.shortValue) : that.shortValue != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = booleanValue != null ? booleanValue.hashCode() : 0;
        result = 31 * result + (byteValue != null ? byteValue.hashCode() : 0);
        result = 31 * result + (shortValue != null ? shortValue.hashCode() : 0);
        result = 31 * result + (integerValue != null ? integerValue.hashCode() : 0);
        result = 31 * result + (longValue != null ? longValue.hashCode() : 0);
        result = 31 * result + (floatValue != null ? floatValue.hashCode() : 0);
        result = 31 * result + (doubleValue != null ? doubleValue.hashCode() : 0);
        result = 31 * result + (primitiveFloat != +0.0f ? Float.floatToIntBits(primitiveFloat) : 0);
        result = 31 * result + (floatList != null ? floatList.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("CoercionBean");
        sb.append("{booleanValue=").append(booleanValue);
        sb.append(", byteValue=").append(byteValue);
        sb.append(", shortValue=").append(shortValue);
        sb.append(", integerValue=").append(integerValue);
        sb.append(", longValue=").append(longValue);
        sb.append(", floatValue=").append(floatValue);
        sb.append(", doubleValue=").append(doubleValue);
        sb.append(", primitiveFloat=").append(primitiveFloat);
        sb.append(", floatList=").append(floatList);
        sb.append('}');
        return sb.toString();
    }
}
