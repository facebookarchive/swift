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


@ThriftStruct(builder=DiscreteComponent.Builder.class)
public interface DiscreteComponent extends Component {

    @ThriftField(2)
    public String getUnits();

    public void setUnits(String units);

    @ThriftField(3)
    public Double getValue();
    
    public void setValue(Double value);

    public static class Builder {

        private String name;
        
        private String units;
        
        private Double value;
        
        public Builder() {
        }

        public String getName() {
            return name;
        }
        
        @ThriftField
        public void setName(String name) {
            this.name = name;
        }

        public String getUnits() {
            return units;
        }

        @ThriftField
        public void setUnits(String units) {
            this.units = units;
        }

        public Double getValue() {
            return value;
        }

        @ThriftField
        public void setValue(Double value) {
            this.value = value;
        }

        @ThriftConstructor
        public DiscreteComponent build() {
            if (getName() != null) {
                if (getName().equals("Resistor")) {
                    return new Resistor(getUnits(), getValue());
                } else if (getName().equals("Capacitor")) {
                    return new Capacitor(getUnits(), getValue());
                }
            }
            return new BasicComponent(getName(), getUnits(), getValue());
        }

    }
    
    public static class Utils {
        
        private Utils() {}
        
        public static int hashCode(DiscreteComponent thi$) {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((thi$.getName() == null) ? 0 : thi$.getName().hashCode());
            result = prime * result + ((thi$.getUnits() == null) ? 0 : thi$.getUnits().hashCode());
            result = prime * result + ((thi$.getValue() == null) ? 0 : thi$.getValue().hashCode());
            return result;
        }
        
        public static boolean areEqual(DiscreteComponent thi$, Object obj) {
            if (thi$ == obj)
                return true;
            if (thi$ == null || obj == null)
                return false;
            if (!DiscreteComponent.class.isAssignableFrom(obj.getClass()))
                return false;
            DiscreteComponent other = (DiscreteComponent) obj;
            if (thi$.getName() == null) {
                if (other.getName() != null)
                    return false;
            } else if (!thi$.getName().equals(other.getName()))
                return false;
            if (thi$.getUnits() == null) {
                if (other.getUnits() != null)
                    return false;
            } else if (!thi$.getUnits().equals(other.getUnits()))
                return false;
            if (thi$.getValue() == null) {
                if (other.getValue() != null)
                    return false;
            } else if (!thi$.getValue().equals(other.getValue()))
                return false;
            return true;
        }

        public static String toString(DiscreteComponent thi$) {
            if (thi$ == null) {
                throw new IllegalArgumentException("component cannot be null");
            }
            StringBuilder b = new StringBuilder().append("Discrete Component(").append(thi$.getClass().getName())
                    .append(") [name=").append(thi$.getName())
                    .append(", units=").append(thi$.getUnits())
                    .append(", value=").append(thi$.getValue())
                    .append("]");
            return b.toString();
        }

    }
    
    public static class BasicComponent implements DiscreteComponent {
        
        private final String name;
        
        private String units;
        
        private Double value;
        
        public BasicComponent(String name, String units, Double value) {
            this.name = name;
            this.units = units;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getUnits() {
            return units;
        }

        public void setUnits(String units) {
            this.units = units;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Utils.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return Utils.areEqual(this, obj);
        }
        
        @Override
        public String toString() {
            return Utils.toString(this);
        }

    }
    
    public static class Resistor extends BasicComponent {

        public Resistor(String units, Double value) {
            super("Resistor", units, value);
        }

    }
    
    public static class Capacitor extends BasicComponent {
        
        public Capacitor(String units, Double value) {
            super("Capacitor", units, value);
        }
        
    }
    
}
