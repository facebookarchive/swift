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

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static java.nio.file.Files.readAllLines;
import static org.junit.Assert.assertTrue;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.2.3", "3.2.5", "3.3.1"})
@SuppressWarnings("JUnitTestNG")
public class SwiftIntegrationTest
{
    @Rule
    public final TestResources resources = new TestResources();

    public final MavenRuntime maven;

    public SwiftIntegrationTest(MavenRuntimeBuilder mavenBuilder)
            throws Exception
    {
        this.maven = mavenBuilder.withCliOptions("-B", "-U").build();
    }

    @Test
    public void testBasic()
            throws Exception
    {
       assertOutcomes("basic", "com/facebook/swift/its/test");
    }
    
    @Test
    public void testShortCircuit() 
        throws Exception
    {
        assertOutcomes("shortcircuit", "com/facebook/swift/its/test");
    }

    @Test
    public void testNamespaceFallback()
        throws Exception
    {
        assertOutcomes("namespace_fallback", "com/facebook/swift/service/scribe");
    }

    private void assertOutcomes(String project, String expectedJavaNamespace)
        throws Exception
    {
        File basedir = resources.getBasedir(project);
        maven.forProject(basedir)
                .execute("generate-sources")
                .assertErrorFreeLog();

        File generated = new File(basedir, "target/generated-sources/swift");
        File output = new File(generated, expectedJavaNamespace);

        assertTrue(new File(output, "LogEntry.java").isFile());
        assertTrue(new File(output, "ResultCode.java").isFile());
        assertTrue(new File(output, "Scribe.java").isFile());

        List<String> lines = readAllLines(output.toPath().resolve("Scribe.java"));
        assertTrue(lines.contains("public interface Scribe"));
    }
}
