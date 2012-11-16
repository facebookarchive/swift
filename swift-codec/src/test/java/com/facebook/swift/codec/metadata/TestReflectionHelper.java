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
package com.facebook.swift.codec.metadata;

import com.facebook.swift.codec.ThriftField;
import org.testng.annotations.Test;

import static com.facebook.swift.codec.metadata.ReflectionHelper.extractParameterNames;
import static org.testng.Assert.assertEquals;

public class TestReflectionHelper
{

    @Test
    public void testExtractParameterNamesNoAnnotations()
            throws Exception
    {
        assertEquals(extractParameterNames(getClass().getDeclaredMethod("noAnnotations", String.class, String.class, String.class)),
                new String[]{"a", "b", "c"});
    }

    private static void noAnnotations(String a, String b, String c)
    {
    }

    @Test
    public void testExtractParameterNamesThriftFieldAnnotation()
            throws Exception
    {
        assertEquals(extractParameterNames(getClass().getDeclaredMethod("thriftFieldAnnotation", String.class, String.class, String.class)),
                new String[]{"a", "b", "c"});
    }

    private static void thriftFieldAnnotation(
            @ThriftField(name = "a") String arg0,
            @ThriftField(name = "b") String arg1,
            @ThriftField(name = "c") String arg2)
    {
    }
}
