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

import java.util.ArrayList;

@ThriftStruct
final public class MyCustomTypeWithCoercedAttributes {

    // Coerced fields types:

    @ThriftField(value = 1)
    public ArrayList<String> arrayListAttribute = new ArrayList<String>();

    @ThriftField(value = 2)
    public MyCustomListType<String> listString = new MyCustomListType<String>("123");
    
    @ThriftField(value = 3)
    public MyCustomListType<Integer> listInt = new MyCustomListType<Integer>(new Integer(12345));

    @ThriftField(value = 4)
    public MyCustomListType<OneOfEverything> listOneEverything = new MyCustomListType<OneOfEverything>(new OneOfEverything());

    @ThriftField(value = 5)
    public MyCustomListType<MyCustomListType<String>> listOfList = new MyCustomListType<MyCustomListType<String>>(new MyCustomListType<String>("123"));

 
    public boolean equals(Object other) {
        MyCustomTypeWithCoercedAttributes o = (MyCustomTypeWithCoercedAttributes)other;
        return o!=null 
                && this.arrayListAttribute.equals(o.arrayListAttribute)
                && this.listString.equals(o.listString)
                && this.listInt.equals(o.listInt)
                && this.listOneEverything.equals(o.listOneEverything)
                && this.listOfList.equals(o.listOfList);
    }
}