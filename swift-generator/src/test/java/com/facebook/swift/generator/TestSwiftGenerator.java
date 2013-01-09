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
package com.facebook.swift.generator;

import java.io.File;

/**
 * Parses a Thrift IDL file and writes out initial annotated java classes.
 */
public class TestSwiftGenerator
{
    private static final String THRIFT2_FOLDER = System.getProperty("user.home") + "/fb/src/swift-demo/src/main/idl";
    private static final String OUTPUT_FOLDER = System.getProperty("user.home") + "/fb/workspace/default/Demo/src/";

    public static void main(final String ... args) throws Exception
    {
        final SwiftGeneratorConfig config = SwiftGeneratorConfig.builder()
                        .inputFolder(new File(THRIFT2_FOLDER))
                        .addInputFiles(
                               new File("hive/metastore.thrift"),
                               new File("Maestro.thrift")
//                               new File("fb303.thrift"),
//                               new File("common/fb303/if/fb303.thrift")
                               )
                        .outputFolder(new File(OUTPUT_FOLDER))
                        .setGenerateIncludedCode()
//                        .clearAddThriftExceptions()
//                        .overridePackage("com.fb.test")
                        .build();

        final SwiftGenerator generator = new SwiftGenerator(config);
        generator.parse();
    }
}
