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
package com.facebook.swift.codec.idlannotations;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftIdlAnnotation;
import com.facebook.swift.codec.ThriftStruct;

@ThriftStruct(
        idlAnnotations = {
                @ThriftIdlAnnotation(key = "testkey1", value = "testvalue1"),
                @ThriftIdlAnnotation(key = "testkey2", value = "testvalue2"),
        }
)
public class StructWithIdlAnnotations
{
    @ThriftField(1)
    public String message;

    @ThriftField(2)
    public int type;
}
