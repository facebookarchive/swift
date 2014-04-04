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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;

@ThriftStruct
public final class IsSetBean
{
    public static IsSetBean createEmpty()
    {
        return new IsSetBean();
    }

    public static IsSetBean createFull()
    {
        IsSetBean isSetBean = new IsSetBean();
        isSetBean.aBoolean = false;
        isSetBean.aByte = 0;
        isSetBean.aShort = 0;
        isSetBean.aInteger = 0;
        isSetBean.aLong = 0L;
        isSetBean.aDouble = 0.0d;
        isSetBean.aString = "";
        isSetBean.aStruct = new BonkField();
        isSetBean.aSet = ImmutableSet.of();
        isSetBean.aList = ImmutableList.of();
        isSetBean.aMap = ImmutableMap.of();

        return isSetBean;
    }

    public Boolean aBoolean;
    public boolean isBooleanSet;

    public Byte aByte;
    public boolean isByteSet;

    public Short aShort;
    public boolean isShortSet;

    public Integer aInteger;
    public boolean isIntegerSet;

    public Long aLong;
    public boolean isLongSet;

    public Double aDouble;
    public boolean isDoubleSet;

    public String aString;
    public boolean isStringSet;

    public BonkField aStruct;
    public boolean isStructSet;

    public Set<String> aSet;
    public boolean isSetSet;

    public List<String> aList;
    public boolean isListSet;

    public Map<String, String> aMap;
    public boolean isMapSet;

    @ThriftField(20)
    public ByteBuffer field = ByteBuffer.wrap("empty".getBytes(UTF_8));

    @ThriftField(1)
    public Boolean getABoolean()
    {
        return aBoolean;
    }

    @ThriftField
    public void setABoolean(Boolean aBoolean)
    {
        this.isBooleanSet = true;
        this.aBoolean = aBoolean;
    }

    @ThriftField(2)
    public Byte getAByte()
    {
        return aByte;
    }

    @ThriftField
    public void setAByte(Byte aByte)
    {
        this.isByteSet = true;
        this.aByte = aByte;
    }

    @ThriftField(3)
    public Short getAShort()
    {
        return aShort;
    }

    @ThriftField
    public void setAShort(Short aShort)
    {
        this.isShortSet = true;
        this.aShort = aShort;
    }

    @ThriftField(4)
    public Integer getAInteger()
    {
        return aInteger;
    }

    @ThriftField
    public void setAInteger(Integer aInteger)
    {
        this.isIntegerSet = true;
        this.aInteger = aInteger;
    }

    @ThriftField(5)
    public Long getALong()
    {
        return aLong;
    }

    @ThriftField
    public void setALong(Long aLong)
    {
        this.isLongSet = true;
        this.aLong = aLong;
    }

    @ThriftField(6)
    public Double getADouble()
    {
        return aDouble;
    }

    @ThriftField
    public void setADouble(Double aDouble)
    {
        this.isDoubleSet = true;
        this.aDouble = aDouble;
    }

    @ThriftField(7)
    public String getAString()
    {
        return aString;
    }

    @ThriftField
    public void setAString(String aString)
    {
        this.isStringSet = true;
        this.aString = aString;
    }

    @ThriftField(8)
    public BonkField getAStruct()
    {
        return aStruct;
    }

    @ThriftField
    public void setAStruct(BonkField aStruct)
    {
        this.isStructSet = true;
        this.aStruct = aStruct;
    }

    @ThriftField(9)
    public Set<String> getASet()
    {
        return aSet;
    }

    @ThriftField
    public void setASet(Set<String> aSet)
    {
        this.isSetSet = true;
        this.aSet = aSet;
    }

    @ThriftField(10)
    public List<String> getAList()
    {
        return aList;
    }

    @ThriftField
    public void setAList(List<String> aList)
    {
        this.isListSet = true;
        this.aList = aList;
    }

    @ThriftField(11)
    public Map<String, String> getAMap()
    {
        return aMap;
    }

    @ThriftField
    public void setAMap(Map<String, String> aMap)
    {
        this.isMapSet = true;
        this.aMap = aMap;
    }

    public boolean isBooleanSet()
    {
        return isBooleanSet;
    }

    public boolean isByteSet()
    {
        return isByteSet;
    }

    public boolean isShortSet()
    {
        return isShortSet;
    }

    public boolean isIntegerSet()
    {
        return isIntegerSet;
    }

    public boolean isLongSet()
    {
        return isLongSet;
    }

    public boolean isDoubleSet()
    {
        return isDoubleSet;
    }

    public boolean isStringSet()
    {
        return isStringSet;
    }

    public boolean isStructSet()
    {
        return isStructSet;
    }

    public boolean isSetSet()
    {
        return isSetSet;
    }

    public boolean isListSet()
    {
        return isListSet;
    }

    public boolean isMapSet()
    {
        return isMapSet;
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

        final IsSetBean isSetBean = (IsSetBean) o;

        if (aBoolean != null ? !aBoolean.equals(isSetBean.aBoolean) : isSetBean.aBoolean != null) {
            return false;
        }
        if (aByte != null ? !aByte.equals(isSetBean.aByte) : isSetBean.aByte != null) {
            return false;
        }
        if (aDouble != null ? !aDouble.equals(isSetBean.aDouble) : isSetBean.aDouble != null) {
            return false;
        }
        if (aInteger != null ? !aInteger.equals(isSetBean.aInteger) : isSetBean.aInteger != null) {
            return false;
        }
        if (aList != null ? !aList.equals(isSetBean.aList) : isSetBean.aList != null) {
            return false;
        }
        if (aLong != null ? !aLong.equals(isSetBean.aLong) : isSetBean.aLong != null) {
            return false;
        }
        if (aMap != null ? !aMap.equals(isSetBean.aMap) : isSetBean.aMap != null) {
            return false;
        }
        if (aSet != null ? !aSet.equals(isSetBean.aSet) : isSetBean.aSet != null) {
            return false;
        }
        if (aShort != null ? !aShort.equals(isSetBean.aShort) : isSetBean.aShort != null) {
            return false;
        }
        if (aString != null ? !aString.equals(isSetBean.aString) : isSetBean.aString != null) {
            return false;
        }
        if (aStruct != null ? !aStruct.equals(isSetBean.aStruct) : isSetBean.aStruct != null) {
            return false;
        }
        if (field != null ? !field.equals(isSetBean.field) : isSetBean.field != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = aBoolean != null ? aBoolean.hashCode() : 0;
        result = 31 * result + (aByte != null ? aByte.hashCode() : 0);
        result = 31 * result + (aShort != null ? aShort.hashCode() : 0);
        result = 31 * result + (aInteger != null ? aInteger.hashCode() : 0);
        result = 31 * result + (aLong != null ? aLong.hashCode() : 0);
        result = 31 * result + (aDouble != null ? aDouble.hashCode() : 0);
        result = 31 * result + (aString != null ? aString.hashCode() : 0);
        result = 31 * result + (aStruct != null ? aStruct.hashCode() : 0);
        result = 31 * result + (aSet != null ? aSet.hashCode() : 0);
        result = 31 * result + (aList != null ? aList.hashCode() : 0);
        result = 31 * result + (aMap != null ? aMap.hashCode() : 0);
        result = 31 * result + (field != null ? field.hashCode() : 0);
        return result;
    }
}
