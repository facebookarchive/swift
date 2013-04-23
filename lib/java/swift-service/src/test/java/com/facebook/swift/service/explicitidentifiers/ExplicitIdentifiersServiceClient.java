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
package com.facebook.swift.service.explicitidentifiers;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import org.apache.thrift.TException;

import java.io.Closeable;

@ThriftService
public interface ExplicitIdentifiersServiceClient extends Closeable
{
    public void close();

    @ThriftMethod
    void explicitParameterOrdering(
            @ThriftField(value = 30) String stringParam,
            @ThriftField(value = 10) int integerParam,
            @ThriftField(value = 20) boolean booleanParam,
            @ThriftField(value = 40) byte dummy)
            throws TException;

    @ThriftMethod
    public void missingIncomingParameter(
            @ThriftField(value = 1) int firstParameter)
            throws TException;

    @ThriftMethod
    public void extraIncomingParameter(
            @ThriftField(value = 1) int firstParameter,
            @ThriftField(value = 2) String secondParameter)
            throws TException;

    @ThriftMethod
    public void missingAndReorderedParameters(
            @ThriftField(value = 1) int integerOne,
            @ThriftField(value = 2) String stringTwo);

    @ThriftMethod
    public void extraAndReorderedParameters(
            @ThriftField(value = 1) int integerOne,
            @ThriftField(value = 2) String stringTwo,
            @ThriftField(value = 3) boolean booleanTrue);

    @ThriftMethod
    public void missingInteger();

    @ThriftMethod
    public void missingStruct();

    @ThriftMethod
    public void extraStruct(CustomArgument customArgument);
}
