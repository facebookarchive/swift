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
package com.facebook.swift.service.annotations;

import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import org.testng.annotations.Test;

// Tests that verify we correctly identify conflicts between inherited interfaces
// annotated with @ThriftService
public class TestThriftServiceAnnotationConflicts
{
    // Passes because only a single ancestor class/interface declares @ThriftService (BaseService)
    //
    // Implementation -- @ThriftService BaseService
    //
    @Test
    public void testInheritBaseInterface()
    {
        ThriftServiceMetadata.getThriftServiceAnnotation(BaseServiceImplementation.class);
    }

    // Passes because even though multiple ancestor class/interfaces declare @ThriftService
    // (BaseService and DerivedServiceOne), BaseService is inherited indirectly through
    // DerivedServiceOne, so DerivedServiceOne's annotation takes precedence.
    //
    // Implementation -- @ThriftService DerivedServiceOne -- @ThriftService BaseService
    //
    @Test
    public void testInheritSingleDerivedInterface()
    {
        ThriftServiceMetadata.getThriftServiceAnnotation(SingleDerivedServiceImplementation.class);
    }

    // Fails because multiple ancestors declare @ThriftService, and there is a conflict between
    // the @ThriftService annotations on DerviceServiceOne and DerivceServiceTwo which cannot
    // be resolved because neither takes precedence over the other
    //
    //                  / @ThriftService DerivedServiceOne \
    // Implementation --                                      -- @ThriftService BaseService
    //                  \ @ThriftService DerivedServiceTwo /
    //
    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testInheritMultipleDerivedInterfaces()
    {
        ThriftServiceMetadata.getThriftServiceAnnotation(MultipleDerivedServiceImplementation.class);
    }

    // Passes because even though the there would be a conflict, the implementation class explicitly
    // declares it's own @ThriftService, overriding all those from ancestors and resolving the conflict
    //
    //                                 / @ThriftService DerivedServiceOne \
    // @ThriftService Implementation --                                    -- @ThriftService BaseService
    //                                 \ @ThriftService DerivedServiceTwo /
    //
    @Test
    public void testInheritMultipleDerviedInterfacesWithExplicitAnnotation()
    {
        ThriftServiceMetadata.getThriftServiceAnnotation(MultipleDerivedServiceImplementationWithExplicitAnnotation.class);
    }

    // Passes because even though multiple ancestors declare @ThriftService, they are all inherited
    // through CombinedService, so it's @ThriftService annotation takes precedence.
    //
    //                                                    / @ThriftService DerivedServiceOne \
    // Implementation -- @ThriftService CombinedService --                                    -- @ThriftService BaseService
    //                                                    \ @ThriftService DerivedServiceTwo /
    //
    @Test
    public void testInheritCombinedInterface()
    {
        ThriftServiceMetadata.getThriftServiceAnnotation(CombinedServiceImplementation.class);
    }
}
