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
package com.facebook.swift.service.exceptions;

import com.facebook.swift.service.ThriftEventHandler;
import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.base.SuiteBase;
import com.google.common.collect.ImmutableList;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ExceptionTest extends SuiteBase<ExceptionService, ExceptionServiceClient>
{
    public ExceptionTest() {
        super(ExceptionServiceHandler.class,
              ExceptionServiceClient.class,
              new ThriftServerConfig(),
              ImmutableList.<ThriftEventHandler>of(new ExceptionThrowingEventHandler()));
    }

    @Test(expectedExceptions = { ThriftCheckedException.class })
    public void testThrowExpectedCheckedException() throws ThriftCheckedException, TException {
        getClient().throwExpectedThriftCheckedException();
    }

    @Test(expectedExceptions = { ThriftUncheckedException.class })
    public void testThrowExceptedUncheckedException() throws ThriftUncheckedException, TException {
        getClient().throwExpectedThriftUncheckedException();
    }

    @Test(expectedExceptions = { TApplicationException.class })
    public void testThrowWrongThriftException() throws TException, ThriftCheckedException {
        getClient().throwWrongThriftException();
    }

    // Doesn't work because even though throwUnexpectedThriftCheckedException doesn't explicitly
    // declare a @ThriftException for the exception type ThriftCheckedException, Swift infers
    // it from the java exception specification, so a ThriftCheckedException is caught.
    @Test(enabled = false, expectedExceptions = { TApplicationException.class })
    public void testThrowUnexpectedThriftCheckedException() throws ThriftCheckedException,
      TException {
        getClient().throwUnexpectedThriftCheckedException();
    }

    @Test(expectedExceptions = { TApplicationException.class })
    public void testThrowUnexpectedThriftUncheckedException() throws TException {
        getClient().throwUnexpectedThriftUncheckedException();
    }

    @Test(expectedExceptions = { TApplicationException.class })
    public void testThrowUnexpectedNonThriftCheckedException() throws TException, NonThriftCheckedException {
        getClient().throwUnexpectedNonThriftCheckedException();
    }

    @Test(expectedExceptions = { TApplicationException.class })
    public void testThrowUnexpectedNonThriftUncheckedException() throws TException {
        getClient().throwUnexpectedNonThriftUncheckedException();
    }

    @Test
    public void testThrowSubclassableException() throws TException {
        try {
            getClient().throwSubclassableException();
            fail("Expected a ThriftCheckedSubclassableException");
        }
        catch (ThriftCheckedSubclassableException e) {
            assertEquals(
                    e.getMessage(),
                    "not subclass",
                    "Expected a 'not subclass' ThriftCheckedSubclassableException");
        }
    }

    @Test
    public void testThrowSubclassOfSubclassableException() throws TException {
        try {
            getClient().throwSubclassOfSubclassableException();
            fail("Expected a ThriftCheckedSubclassableException");
        }
        catch (ThriftCheckedSubclassableException e) {
            assertEquals(
                    e.getMessage(),
                    "is subclass",
                    "Expected a 'is subclass' ThriftCheckedSubclassableException");
            assertEquals(
                    e.getClass(),
                    ThriftCheckedSubclassableException.class,
                    "Expected TCSE.Subclass to get serialized as a TCSE");
        }
    }

    @Test(expectedExceptions = { TApplicationException.class })
    public void testThrowExceptionInEventHandlersCode() throws TException {
      getClient().throwExceptionInEventHandlersCode();
    }

    @Test
    public void testMissingMethod() {
        try {
            getClient().missingMethod();
            fail("Expected TApplicationException of type UNKNOWN_METHOD");
        }
        catch (TApplicationException e) {
            assertEquals(
                    e.getType(),
                    TApplicationException.UNKNOWN_METHOD,
                    "Expected TApplicationException of type UNKNOWN_METHOD");
        }
    }

    /**
     * This class will be used to test if event handlers' exceptions are propagated back to the client.
     */
    private static class ExceptionThrowingEventHandler extends ThriftEventHandler {
        @Override
        public void preRead(Object handlerContext, String methodName) throws TApplicationException {
            if ("ExceptionServiceHandler.throwExceptionInEventHandlersCode".equals(methodName)) {
                throw new TApplicationException(
                        "This is an exception for testing if event handler exceptions propagate to the client ");
            }
        }
    }

}
