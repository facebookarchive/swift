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

import static org.fest.assertions.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.metadata.ReflectionHelper;
import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftFieldMetadata;
import com.facebook.swift.service.ThriftMethod;

public class TestThriftMethodLegacyIds
{
    @DataProvider
    public Object[][] getTestCasesWithLegacyFieldIds()
    {
        List<Object[]> cases = new ArrayList<>();
        for (Method m : ReflectionHelper.findAnnotatedMethods(DummyService.class, ThriftMethod.class)) {
            String name = m.getName();
            if (name.startsWith("validNonLegacy")) {
                continue;
            } else if (name.startsWith("validLegacy")) {
                cases.add(new Object[] { new Method[] { m } });
            } else if (name.startsWith("invalid")) {
                continue;
            } else {
                throw new AssertionError("Weird method " + m);
            }
        }
        return cases.toArray(new Object[][] {});
    }

    @Test(dataProvider="getTestCasesWithLegacyFieldIds")
    public void testLegacyFieldIds(Method[] mBox)
    {
        Method m = mBox[0];
        ThriftMethodMetadata metadata = new ThriftMethodMetadata("DummyService", m, new ThriftCatalog());
        List<ThriftFieldMetadata> parameters = metadata.getParameters();

        assertThat(parameters)
                .as("parameters")
                .hasSize(1);

        assertThat(parameters.get(0).getId())
                .as("the parameter's ID")
                .isNegative();
    }

    @DataProvider
    public Object[][] getTestCasesWithNonLegacyFieldIds()
    {
        List<Object[]> cases = new ArrayList<>();
        for (Method m : ReflectionHelper.findAnnotatedMethods(DummyService.class, ThriftMethod.class)) {
            String name = m.getName();
            if (name.startsWith("validNonLegacy")) {
                cases.add(new Object[] { new Method[] { m } });
            } else if (name.startsWith("validLegacy")) {
                continue;
            } else if (name.startsWith("invalid")) {
                continue;
            } else {
                throw new AssertionError("Weird method " + m);
            }
        }
        return cases.toArray(new Object[][] {});
    }

    @Test(dataProvider="getTestCasesWithNonLegacyFieldIds")
    public void testNonLegacyFieldIds(Method[] mBox)
    {
        Method m = mBox[0];
        ThriftMethodMetadata metadata = new ThriftMethodMetadata("DummyService", m, new ThriftCatalog());
        List<ThriftFieldMetadata> parameters = metadata.getParameters();

        assertThat(parameters)
                .as("parameters")
                .hasSize(1);

        assertThat(parameters.get(0).getId())
                .as("the parameter's ID")
                .isGreaterThanOrEqualTo((short) 0);
    }

   @DataProvider
   public Object[][] getTestCasesWithInvalids()
   {
       List<Object[]> cases = new ArrayList<>();
       for (Method m : ReflectionHelper.findAnnotatedMethods(DummyService.class, ThriftMethod.class)) {
           String name = m.getName();
           if (name.startsWith("validNonLegacy")) {
               continue;
           } else if (name.startsWith("validLegacy")) {
               continue;
           } else if (name.startsWith("invalid")) {
               cases.add(new Object[] { new Method[] { m } });
           } else {
               throw new AssertionError("Weird method " + m);
           }
       }
       return cases.toArray(new Object[][] {});
   }

   @Test(
       dataProvider = "getTestCasesWithInvalids",
       expectedExceptions = IllegalArgumentException.class,
       expectedExceptionsMessageRegExp = "isLegacyId (must|should only) be specified.*"
   )
   public void testInvalids(Method[] mBox)
   {
       Method m = mBox[0];
       new ThriftMethodMetadata("DummyService", m, new ThriftCatalog());
   }


    public static interface DummyService {
        @ThriftMethod
        public void validNonLegacyFieldId1(@ThriftField(isLegacyId=false) boolean blah);

        @ThriftMethod
        public void validNonLegacyFieldId2(@ThriftField(4) boolean blah);

        @ThriftMethod
        public void validNonLegacyFieldId3(@ThriftField(value=4, isLegacyId=false) boolean blah);

        @ThriftMethod
        public void invalidSaysLegacyButShouldnt1(@ThriftField(value=4, isLegacyId=true) boolean blah);

        @ThriftMethod
        public void invalidSaysLegacyButShouldnt2(@ThriftField(isLegacyId=true) boolean blah);


        @ThriftMethod
        public void validLegacyFieldId(@ThriftField(value=-4, isLegacyId=true) boolean blah);

        @ThriftMethod
        public void invalidIsLegacyButDoesntSay(@ThriftField(value=-4) boolean blah);

        @ThriftMethod
        public void invalidIsLegacyButSaysNot(@ThriftField(value=-4, isLegacyId=false) boolean blah);
    }
}
