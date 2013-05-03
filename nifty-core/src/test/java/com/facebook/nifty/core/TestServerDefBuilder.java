/*
 * Copyright (C) 2012-2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.nifty.core;

import com.facebook.nifty.processor.NiftyProcessor;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestServerDefBuilder
{
    @Test
    public void testServerDefBuilderWithoutProcessor()
    {
        try {
            new ThriftServerDefBuilder().build();
        }
        catch (Exception e) {
            return;
        }
        Assert.fail();
    }

    @Test
    public void testServerDefBuilder()
    {
        try {
            new ThriftServerDefBuilder()
                    .withProcessor(EasyMock.createMock(NiftyProcessor.class))
                    .build();
        }
        catch (Exception e) {
            Assert.fail();
        }
    }
}
