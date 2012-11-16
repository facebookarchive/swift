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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;

public class TestBytecodeReaderParanamer
{
    @Test
    public void testExtractPrimiteAndArrayParameterNames() throws Exception
    {
        BytecodeReadingParanamer paranamer = new BytecodeReadingParanamer();
        Constructor<Foo> constructor = Foo.class.getConstructor(
                String.class, String[].class,
                Foo.class, Foo[].class,
                boolean.class, boolean[].class,
                char.class, char[].class,
                byte.class, byte[].class,
                short.class, short[].class,
                int.class, int[].class,
                long.class, long[].class,
                float.class, float[].class,
                double.class, double[].class
        );
        String[] parameterNames = paranamer.lookupParameterNames(constructor);
        String[] expectedParameterNames = new String[]{
                "bar1", "bar2", "bar3", "bar4", "bar5", "bar6", "bar7", "bar8", "bar9", "bar10",
                "bar11", "bar12", "bar13", "bar14", "bar15", "bar16", "bar17", "bar18", "bar19", "bar20"
        };

        Assert.assertEquals(parameterNames, expectedParameterNames);
    }

    public static class Foo
    {
        public Foo(
                String bar1, String[] bar2,
                Foo bar3, Foo[] bar4,
                boolean bar5, boolean[] bar6,
                char bar7, char[] bar8,
                byte bar9, byte[] bar10,
                short bar11, short[] bar12,
                int bar13, int[] bar14,
                long bar15, long[] bar16,
                float bar17, float[] bar18,
                double bar19, double[] bar20
        )
        {
        }
    }
}
