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
package com.facebook.swift.service.metadata;

import com.facebook.swift.codec.ThriftStruct;
import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import org.apache.thrift.TException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

// TODO add more tests... (currently only tests exception metadata)
public class TestThriftMethodMetadata
{
    @Test
    public void testNoExceptions() throws Exception
    {
        assertExceptions("noExceptions");
    }

    @Test
    public void testAnnotatedExceptions() throws Exception
    {
        assertExceptions("annotatedExceptions", ExceptionA.class, ExceptionB.class);
    }

    @Test
    public void testInferredException() throws Exception
    {
        assertExceptions("inferredException", ExceptionA.class);
    }

    @Test
    public void testInferredExceptionWithTException() throws Exception
    {
        assertExceptions("inferredExceptionWithTException", ExceptionA.class);
    }

    @Test
    public void testInferredExceptionWithRuntimeException() throws Exception
    {
        assertExceptions("inferredExceptionWithRuntimeException", ExceptionA.class);
    }

    @Test
    public void testInferredExceptionWithRuntimeAndTException() throws Exception
    {
        assertExceptions("inferredExceptionWithRuntimeAndTException", ExceptionA.class);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "ThriftMethod.exception annotation value must be specified when more than one custom exception is thrown.")
    public void testUninferrableException() throws Exception
    {
        assertExceptions("uninferrableException");
    }

    private void assertExceptions(String methodName, Class<? extends Exception>... expectedExceptions) throws Exception
    {
        ThriftMethodMetadata metadata = new ThriftMethodMetadata("DummyService", DummyService.class.getMethod(methodName), new ThriftCatalog());
        Map<Short, Type> actualIdMap = new TreeMap<>();
        Map<Short, Type> expectedIdMap = new TreeMap<>();

        for (Map.Entry<Short, ThriftType> entry : metadata.getExceptions().entrySet()) {
            actualIdMap.put(entry.getKey(), entry.getValue().getJavaType());
        }

        short expectedId = 1;

        for (Class<? extends Exception> expectedException : expectedExceptions) {
            expectedIdMap.put(expectedId, expectedException);
            ++expectedId;
        }

        // string comparison produces more useful failure message (and is safe, given the types)
        Assert.assertEquals(actualIdMap.toString(), expectedIdMap.toString());
    }

    @SuppressWarnings("unused")
    public static interface DummyService
    {
        @ThriftMethod
        public void noExceptions();

        @ThriftMethod(
                exception = {
                        @ThriftException(id = 1, type = ExceptionA.class),
                        @ThriftException(id = 2, type = ExceptionB.class)
                }
        )
        public void annotatedExceptions() throws ExceptionA, ExceptionB;

        @ThriftMethod
        public void inferredException() throws ExceptionA;

        @ThriftMethod
        public void inferredExceptionWithTException() throws ExceptionA, TException;

        @ThriftMethod
        public void inferredExceptionWithRuntimeException() throws IllegalArgumentException, ExceptionA;

        @ThriftMethod
        public void inferredExceptionWithRuntimeAndTException() throws IllegalArgumentException, ExceptionA, TException;

        @ThriftMethod(exception = {@ThriftException(id = 1, type = ExceptionA.class)})
        public void uninferrableException() throws ExceptionA, ExceptionB;
    }

    @ThriftStruct
    public final static class ExceptionA extends Exception
    {
        private static final long serialVersionUID = 1L;
    }

    @ThriftStruct
    public final static class ExceptionB extends Exception
    {
        private static final long serialVersionUID = 1L;
    }
}
