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

/**
 * For testing access to 3rd party collection classes (e.g. scala).
 * 
 * NB. No @ThriftStruct annotation here. Simulated 3rd party list type.
 */
public class MyCustomListType<T> {
    public ArrayList<T> storage = new ArrayList<T>();

    public MyCustomListType() {
    }

    public MyCustomListType(T element) {
        storage.add(element);
        storage.add(element);
    }

    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
        if (other == null || !(other instanceof MyCustomListType)) {
            return false;
        }
        return storage.equals(((MyCustomListType<T>) other).storage);
    }
}