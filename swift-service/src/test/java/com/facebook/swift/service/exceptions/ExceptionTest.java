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

import com.facebook.swift.service.base.SuiteBase;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ExceptionTest extends SuiteBase<ExceptionService, ExceptionServiceClient>
{
    public ExceptionTest() {
        super(ExceptionServiceHandler.class, ExceptionServiceClient.class);
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
}
