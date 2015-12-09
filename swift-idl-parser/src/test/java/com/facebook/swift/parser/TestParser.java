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
import com.google.common.io.Files;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Iterator;

import static com.facebook.swift.testing.TestingUtils.getResourcePath;
import static com.facebook.swift.testing.TestingUtils.listDataProvider;
import static com.facebook.swift.testing.TestingUtils.listMatchingFiles;
import static com.facebook.swift.parser.ThriftIdlParser.parseThriftIdl;

public class TestParser
{
    @DataProvider
    public Iterator<Object[]> thriftProvider()
            throws Exception
    {
        return listDataProvider(listMatchingFiles(getResourcePath(""), "**/*.thrift"));
    }

    @Test(dataProvider = "thriftProvider")
    public void testParse(Path path)
            throws Exception
    {
        parseThriftIdl(pathReader(path));
    }

    @Parameters("sampleDirectory")
    @Test(groups = "sample")
    public void testParseSample(String sampleDirectory)
            throws Exception
    {
        Path directory = FileSystems.getDefault().getPath(sampleDirectory);
        for (Path path : listMatchingFiles(directory, "**/*.thrift")) {
            if (path.endsWith("BrokenConstants.thrift")) {
                System.out.println("skipping file: " + path);
                continue;
            }
            System.out.println("parsing file: " + path);
            parseThriftIdl(pathReader(path));
        }
    }

    private static CharSource pathReader(Path path)
    {
        return Files.asByteSource(path.toFile()).asCharSource(Charsets.UTF_8);
    }
}
