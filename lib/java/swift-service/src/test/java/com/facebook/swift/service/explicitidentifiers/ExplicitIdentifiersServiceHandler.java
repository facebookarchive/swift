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
import com.google.common.base.Optional;

@ThriftService
public class ExplicitIdentifiersServiceHandler
{
    private Object lastStringParam;
    private Object lastBooleanParam;
    private Object lastIntegerParam;
    private Object lastByteParam;
    private Object lastCustomParam;

    private void initializeLastParamValues()
    {
        this.lastBooleanParam = null;
        this.lastStringParam = null;
        this.lastByteParam = null;
        this.lastCustomParam = null;
        this.lastIntegerParam = null;
    }

    @ThriftMethod
    public void explicitParameterOrdering(
            @ThriftField(value = 20) boolean booleanParam,
            @ThriftField(value = 30) String stringParam,
            @ThriftField(value = 10) int integerParam,
            @ThriftField(value = 40) byte byteParam)
    {
        initializeLastParamValues();
        this.lastBooleanParam = Optional.of(booleanParam);
        this.lastIntegerParam = Optional.of(integerParam);
        this.lastByteParam = Optional.of(byteParam);
        this.lastStringParam = Optional.fromNullable(stringParam);
    }

    @ThriftMethod
    public void missingIncomingParameter(
            @ThriftField(value = 1) int integerParam,
            @ThriftField(value = 2) String stringParam)
    {
        initializeLastParamValues();
        this.lastIntegerParam = Optional.of(integerParam);
        this.lastStringParam = Optional.fromNullable(stringParam);
    }

    @ThriftMethod
    public void extraIncomingParameter(
            @ThriftField(value = 1) int integerParam)
    {
        initializeLastParamValues();
        this.lastIntegerParam = Optional.of(integerParam);
    }

    @ThriftMethod
    public void missingAndReorderedParameters(
            @ThriftField(value = 3) boolean booleanParam,
            @ThriftField(value = 2) String stringParam,
            @ThriftField(value = 1) int integerParam)
    {
        initializeLastParamValues();
        this.lastBooleanParam = Optional.of(booleanParam);
        this.lastIntegerParam = Optional.of(integerParam);
        this.lastStringParam = Optional.fromNullable(stringParam);
    }

    @ThriftMethod
    public void extraAndReorderedParameters(
            @ThriftField(value = 3) boolean booleanParam,
            @ThriftField(value = 2) String stringParam)
    {
        initializeLastParamValues();
        this.lastBooleanParam = Optional.of(booleanParam);
        this.lastStringParam = Optional.fromNullable(stringParam);
    }

    @ThriftMethod
    public void missingInteger(
            @ThriftField(value = 1) int integerParam)
    {
        initializeLastParamValues();
        this.lastIntegerParam = Optional.of(integerParam);
    }

    @ThriftMethod
    public void missingStruct(
            @ThriftField(value = 1) CustomArgument customParam)
    {
        initializeLastParamValues();
        this.lastCustomParam = Optional.fromNullable(customParam);
    }

    @ThriftMethod
    public void extraStruct()
    {
        initializeLastParamValues();
    }

    public boolean hasLastBooleanParam()
    {
        return lastBooleanParam != null;
    }

    public boolean hasLastStringParam()
    {
        return lastStringParam != null;
    }

    public boolean hasLastIntegerParam()
    {
        return lastIntegerParam != null;
    }

    public boolean hasLastByteParam()
    {
        return lastByteParam != null;
    }

    public boolean hasLastCustomParam()
    {
        return lastCustomParam != null;
    }

    public Optional<Boolean> getLastBooleanParam()
    {
        return (Optional<Boolean>)lastBooleanParam;
    }

    public Optional<String> getLastStringParam()
    {
        return (Optional<String>)lastStringParam;
    }

    public Optional<Integer> getLastIntegerParam()
    {
        return (Optional<Integer>)lastIntegerParam;
    }

    public Optional<Byte> getLastByteParam()
    {
        return (Optional<Byte>)lastByteParam;
    }

    public Optional<CustomArgument> getLastCustomParam()
    {
        return ((Optional<CustomArgument>)lastCustomParam);
    }

    public void setLastBooleanParam(boolean lastBooleanParam)
    {
        this.lastBooleanParam = Optional.of(lastBooleanParam);
    }

    public void setLastIntegerParam(int lastIntegerParam)
    {
        this.lastIntegerParam = Optional.of(lastIntegerParam);
    }

    public void setLastByteParam(byte lastByteParam)
    {
        this.lastByteParam = Optional.of(lastByteParam);
    }

    public void setLastStringParam(String lastStringParam)
    {
        this.lastStringParam = Optional.fromNullable(lastStringParam);
    }

    public void setLastCustomParam(CustomArgument lastCustomParam)
    {
        this.lastCustomParam = Optional.fromNullable(lastCustomParam);
    }
}
