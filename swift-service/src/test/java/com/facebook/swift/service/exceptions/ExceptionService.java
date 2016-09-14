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

import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import org.apache.thrift.TException;

@ThriftService
public interface ExceptionService
{

    @ThriftMethod(exception = { @ThriftException(type = ThriftCheckedException.class, id = 1) })
    public void throwExpectedThriftCheckedException() throws ThriftCheckedException,
      TException;

    @ThriftMethod(exception = { @ThriftException(type = ThriftUncheckedException.class, id = 1) })
    public void throwExpectedThriftUncheckedException() throws TException;

    @ThriftMethod(exception = { @ThriftException(type = ThriftCheckedException.class, id = 1) })
    public void throwWrongThriftException() throws TException, ThriftCheckedException;

    @ThriftMethod
    public void throwUnexpectedThriftCheckedException() throws ThriftCheckedException,
      TException;

    @ThriftMethod
    public void throwUnexpectedThriftUncheckedException() throws TException;

    @ThriftMethod
    public void throwUnexpectedNonThriftCheckedException() throws TException,
      NonThriftCheckedException;

    @ThriftMethod
    public void throwUnexpectedNonThriftUncheckedException() throws TException;

    @ThriftMethod(exception = { @ThriftException(type = ThriftCheckedSubclassableException.class, id = 1) })
    public void throwSubclassableException() throws ThriftCheckedSubclassableException, TException;

    @ThriftMethod(exception = { @ThriftException(type = ThriftCheckedSubclassableException.class, id = 1) })
    public void throwSubclassOfSubclassableException() throws ThriftCheckedSubclassableException, TException;

    @ThriftMethod
    public void throwExceptionInEventHandlersCode() throws TException;

}
