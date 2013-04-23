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

import com.facebook.swift.service.base.SuiteBase;
import org.apache.thrift.TException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class ExplicitIdentifiersTest extends SuiteBase<ExplicitIdentifiersServiceHandler, ExplicitIdentifiersServiceClient>
{
    public ExplicitIdentifiersTest()
    {
        super(ExplicitIdentifiersServiceHandler.class, ExplicitIdentifiersServiceClient.class);
    }

    // Client passes all parameters, but in a different order than the server expects
    @Test
    public void testExplicitParameterOrdering()
            throws TException
    {
        int integerParam = Integer.MAX_VALUE;
        byte byteParam = Byte.MAX_VALUE;
        Boolean booleanParam = Boolean.TRUE;

        getClient().explicitParameterOrdering("STRING", integerParam, booleanParam, byteParam);

        assertEquals(getHandler().getLastIntegerParam().get(), (Integer) integerParam);
        assertEquals(getHandler().getLastBooleanParam().get(), booleanParam);
        assertEquals(getHandler().getLastByteParam().get(), (Byte) byteParam);
        assertEquals(getHandler().getLastStringParam().get(), "STRING");

        // other params are not sent or received
        assertFalse(getHandler().hasLastCustomParam());
    }

    // Client passes only one parameter, server expects two
    @Test
    public void testMissingParameter()
            throws TException
    {
        getClient().missingIncomingParameter(1);

        assertEquals(getHandler().getLastIntegerParam().get(), (Integer) 1);

        // integer should be received by the handler
        assertEquals(getHandler().getLastIntegerParam().get(), (Integer) 1);

        // string is not passed, so thrift will fill in the default value for the handler
        assertFalse(getHandler().getLastStringParam().isPresent());

        // other params are neither sent nor received
        assertFalse(getHandler().hasLastBooleanParam());
        assertFalse(getHandler().hasLastByteParam());
        assertFalse(getHandler().hasLastCustomParam());
    }

    // Client passes two parameters, server expects only one
    @Test
    public void testExtraParameter()
            throws TException
    {
        getClient().extraIncomingParameter(1, "2");

        // integer should be received by handler
        assertEquals(getHandler().getLastIntegerParam().get(), (Integer) 1);

        // string is an extra that is sent, but not received by handler
        assertFalse(getHandler().hasLastStringParam());

        // other params are not sent or received
        assertFalse(getHandler().hasLastBooleanParam());
        assertFalse(getHandler().hasLastByteParam());
        assertFalse(getHandler().hasLastCustomParam());
    }

    // Client passes two parameters in ID order (1,2), server expects three in ID order (3,2,1)
    @Test
    public void testMissingAndReorderedParameters()
    {
        getClient().missingAndReorderedParameters(1, "2");

        // integer and string should be received by handler
        assertEquals(getHandler().getLastIntegerParam().get(), (Integer) 1);
        assertEquals(getHandler().getLastStringParam().get(), "2");

        // bool is not sent, so thrift will fill in the default value for the handler
        assertEquals(getHandler().getLastBooleanParam().get(), Boolean.FALSE);

        // other params are not sent or received
        assertFalse(getHandler().hasLastByteParam());
        assertFalse(getHandler().hasLastCustomParam());
    }

    // Client passes three parameters in ID order (1,2,3), server expects two in ID order (3,2)
    @Test
    public void testExtraAndReorderedParameters()
    {
        getClient().extraAndReorderedParameters(1, "2", true);

        // bool and string should be received by handler
        assertEquals(getHandler().getLastStringParam().get(), "2");
        assertEquals(getHandler().getLastBooleanParam().get(), Boolean.TRUE);

        // integer is an extra param that is sent, but not received by handler
        assertFalse(getHandler().hasLastIntegerParam());

        // other params are not sent or received
        assertFalse(getHandler().hasLastByteParam());
        assertFalse(getHandler().hasLastCustomParam());
    }

    @Test
    public void testMissingInteger()
    {
        getClient().missingInteger();

        // int is not sent, but is synthesized by thrift to the default value (0)
        assertEquals(getHandler().getLastIntegerParam().get(), (Integer) 0);

        // other params are not sent or received
        assertFalse(getHandler().hasLastByteParam());
        assertFalse(getHandler().hasLastCustomParam());
        assertFalse(getHandler().hasLastStringParam());
        assertFalse(getHandler().hasLastBooleanParam());
    }

    @Test
    public void testMissingStruct()
    {
        getClient().missingStruct();

        // struct is not sent, but is synthesized by thrift to the default value (null)
        assertFalse(getHandler().getLastCustomParam().isPresent());

        // other params are not sent or received
        assertFalse(getHandler().hasLastByteParam());
        assertFalse(getHandler().hasLastStringParam());
        assertFalse(getHandler().hasLastIntegerParam());
        assertFalse(getHandler().hasLastBooleanParam());
    }

    @Test
    public void testExtraStruct()
    {
        getClient().extraStruct(new CustomArgument(1, "2"));

        // struct is sent, but is not received by the handler
        assertFalse(getHandler().hasLastCustomParam());

        // other params are not sent or received
        assertFalse(getHandler().hasLastByteParam());
        assertFalse(getHandler().hasLastStringParam());
        assertFalse(getHandler().hasLastIntegerParam());
        assertFalse(getHandler().hasLastBooleanParam());
    }
}
