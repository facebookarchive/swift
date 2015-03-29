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

@ThriftStruct
public final class ComponentWithMultipleAnnotatedInterfaces 
    implements DigitalComponent, DiscreteComponent {

    private String units;
    
    private Double value;
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUnits() {
        return units;
    }

    @Override
    public void setUnits(String units) {
        this.units = units;
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    @Override
    public void setValue(Double value) {
        this.value = value;
    }

    @Override
    public String getPackage() {
        return "Fedex";
    }

    @Override
    public String getManufacturer() {
        return "Acme";
    }

    @Override
    public String getPartNumber() {
        return "555555555";
    }

    
    
}
