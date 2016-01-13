/*
 * Copyright (C) 2015 Facebook, Inc.
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
package com.facebook.mojo;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwiftMojoTest
{

    @Test
    public void posixFormat()
    {
        assertTrue(canBypassScan("foo", "bar", "bar/foo/coo"));
        assertTrue(canBypassScan("foo/bar/./bar"));
        assertFalse(canBypassScan());
        assertFalse(canBypassScan("foo", "???bar.java"));
        assertFalse(canBypassScan("foo/**/*/java", "bar"));
        assertFalse(canBypassScan("foo", "bar/../../foo"));
        assertFalse(canBypassScan("foo", "bar/"));
        assertFalse(canBypassScan("/foo", "bar"));
    }

    @Test
    public void windowsFormat()
    {
        assertTrue(canBypassScan("foo", "bar", "bar\\foo\\coo"));
        assertTrue(canBypassScan("foo\\bar\\.\\bar"));
        assertFalse(canBypassScan());
        assertFalse(canBypassScan("foo", "???bar.java"));
        assertFalse(canBypassScan("foo\\**\\*\\java", "bar"));
        assertFalse(canBypassScan("foo", "bar\\..\\..\\foo"));
        assertFalse(canBypassScan("foo", "bar\\"));
        assertFalse(canBypassScan("\\foo", "bar"));
    }

    private boolean canBypassScan(String... pattern)
    {
        return SwiftMojo.canBypassScan(Arrays.asList(pattern), Collections.emptyList());
    }
}
