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

import java.nio.file.Path;
import java.util.Iterator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static com.facebook.swift.testing.TestingUtils.getResourcePath;
import static com.facebook.swift.testing.TestingUtils.listDataProvider;
import static com.facebook.swift.testing.TestingUtils.listMatchingFiles;
import com.google.common.io.Resources;
import java.io.File;
import java.util.Collections;
import org.testng.annotations.BeforeClass;

public class SwiftGeneratorTest {

    private static final String outputFolderRoot = System.getProperty("test.output.directory");
    
    @BeforeClass
    public static void ensureRootFolder() {
        new File(outputFolderRoot).mkdirs();
    }
    
    @DataProvider
    public Iterator<Object[]> thriftProvider()
            throws Exception {
        return listDataProvider(listMatchingFiles(getResourcePath(""), "**/*.thrift"));
    }

    @Test(dataProvider = "thriftProvider")
    public void testGenerate(Path path) throws Exception {

        // Create a nice output directory for these generated files
        Path rootPath = getResourcePath("");
        Path relativePath = rootPath.relativize(path);
        String testPath = relativePath.toString().replace(
                relativePath.getFileSystem().getSeparator(), 
                "_");
        File outputFolder = new File(outputFolderRoot, testPath);

        final SwiftGeneratorConfig config = SwiftGeneratorConfig.builder()
                .inputBase(Resources.getResource(TestSwiftGenerator.class, "/").toURI())
                .outputFolder(outputFolder)
                .generateIncludedCode(true)
                .codeFlavor("java-immutable")
                .defaultPackage("com.facebook.swift")
                .addTweak(SwiftGeneratorTweak.ADD_CLOSEABLE_INTERFACE)
                .addTweak(SwiftGeneratorTweak.EXTEND_RUNTIME_EXCEPTION)
                .addTweak(SwiftGeneratorTweak.ADD_THRIFT_EXCEPTION)
                .addTweak(SwiftGeneratorTweak.USE_PLAIN_JAVA_NAMESPACE)
                .build();

        final SwiftGenerator generator = new SwiftGenerator(config);
        generator.parse(Collections.singletonList(path.toUri()));

    }

}
