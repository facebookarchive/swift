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
package com.facebook.swift.parser;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import org.testng.annotations.Test;

import static com.facebook.swift.parser.ThriftIdlParser.parseThriftIdl;
import static com.facebook.swift.parser.ThriftIdlParser.parseTree;
import static com.facebook.swift.parser.TreePrinter.treeToString;

public class TestDocument
{
    @Test
    public void testEmpty()
            throws Exception
    {
        parseThriftIdl(CharSource.wrap(""));
    }

    @Test
    public void testDocumentFb303()
            throws Exception
    {
        // TODO: verify document
        System.out.println(treeToString(parseTree(resourceReader("fb303.thrift"))));
        System.out.println(parseThriftIdl(resourceReader("fb303.thrift")));
    }

    @Test
    public void testDocumentHbase()
            throws Exception
    {
        // TODO: verify document
        System.out.println(treeToString(parseTree(resourceReader("Hbase.thrift"))));
        System.out.println(parseThriftIdl(resourceReader("Hbase.thrift")));
    }

    private static CharSource resourceReader(String name)
    {
        return Resources.asCharSource(Resources.getResource(name), Charsets.UTF_8);
    }
}
