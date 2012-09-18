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
