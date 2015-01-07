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
import java.util.List;

import com.facebook.swift.codec.internal.coercion.FromThrift;
import com.facebook.swift.codec.internal.coercion.ToThrift;

/**
 * NB. These annotations are not used.
 */
public class MyCustomCoericions {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @ToThrift
    public final static ArrayList<?> toThrift_ArrayList(java.util.List<?> value) {
        if (value instanceof ArrayList<?>) {
            return (ArrayList<?>) value;
        }

        ArrayList result = new ArrayList(value.size());
        int i = 0;
        while (i < value.size()) {
            result.set(i, value.get(i));
            i += 1;
        }
        return result;
    }

    @FromThrift
    public final static List<?> fromThrift_ArrayList(ArrayList<?> value) {
        return value;
    }

    @ToThrift
    public final static ArrayList<?> toThrift_MyCustomList(MyCustomListType<?> value) {
        return value.storage;
    }

    @SuppressWarnings("unchecked")
    @FromThrift
    public final static MyCustomListType<?> fromThrift_MyCustomList(ArrayList<?> value) {
        MyCustomListType<Object> result = new MyCustomListType<Object>();
        result.storage = (ArrayList<Object>) value;
        return result;
    }
}