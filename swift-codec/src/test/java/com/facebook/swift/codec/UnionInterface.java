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

import com.facebook.swift.codec.UnionInterface.Builder;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@ThriftUnion(builder=Builder.class)
public interface UnionInterface {

    @ThriftUnionId
    public short getType();
    
    @ThriftField(1)
    public String getStringValue();
    
    @ThriftField(2)
    public Long getLongValue();
    
    @ThriftField(3)
    public Fruit getFruitValue();
    
    public Object getValue();

    public static final class Builder {

        private Object value;
        
        private short type;

        @ThriftConstructor
        public UnionInterface build() {
            return createInstance(type, value);
        }
        
        @ThriftField
        public void setStringValue(String value) {
            this.type = 1;
            this.value = value;
        }

        @ThriftField
        public void setLongValue(Long value) {
            this.type = 2;
            this.value = value;
        }

        @ThriftField
        public void setFruitValue(Fruit value) {
            this.type = 3;
            this.value = value;
        }

        public static UnionInterface createInstance(String stringValue) {
            return createInstance((short) 1, stringValue);
        }
        
        public static UnionInterface createInstance(Long longValue) {
            return createInstance((short) 2, longValue);
        }
        
        public static UnionInterface createInstance(Fruit fruitValue) {
            return createInstance((short) 3, fruitValue);
        }
        
        private static UnionInterface createInstance(final short _type, final Object _value) {
            return new UnionInterface() {

                @Override
                public short getType() {
                    return _type;
                }

                @Override
                public String getStringValue() {
                    if (_type != 1) throw new IllegalStateException("type is not a string");
                    return (String) _value;
                }

                @Override
                public Long getLongValue() {
                    if (_type != 2) throw new IllegalStateException("type is not a long");
                    return (Long) _value;
                }

                @Override
                public Fruit getFruitValue() {
                    if (_type != 3) throw new IllegalStateException("type is not a fruit");
                    return (Fruit) _value;
                }
                
                public Object getValue() {
                    return _value;
                }
                
                @Override
                public int hashCode()
                {
                    return Objects.hashCode(_value, _type);
                }

                @Override
                public boolean equals(Object obj)
                {
                    if (this == obj) {
                        return true;
                    }
                    else if (obj == null || !UnionInterface.class.isAssignableFrom(obj.getClass())) {
                        return false;
                    }

                    UnionInterface that = (UnionInterface) obj;
                    return Objects.equal(this.getType(), that.getType())
                        && Objects.equal(this.getValue(), that.getValue());
                }

                @Override
                public String toString()
                {
                    ToStringHelper helper = Objects.toStringHelper(this);
                    if (_type == 1) {
                        helper.add("stringValue", (String) _value);
                    }
                    else if (_type == 2) {
                        helper.add("longValue", (Long) _value);
                    }
                    else if (_type == 3) {
                        helper.add("fruitValue", (Fruit) _value);
                    }
                    return helper.toString();
                }

            };
        }

    }

}
