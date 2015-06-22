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
public class Swift2ThriftIntegrationTest {
    public static final String IDL_FILE = "swift2thrift-generated.thrift";
    @Rule
    public final TestResources resources = new TestResources();

    public final MavenRuntime maven;

    public Swift2ThriftIntegrationTest(MavenRuntimeBuilder mavenBuilder)
            throws Exception {
        this.maven = mavenBuilder.withCliOptions("-B", "-U").build();
    }

    @Test
    public void testBasic()
            throws Exception {
       File basedir = resources.getBasedir("basic");
        maven.forProject(basedir)
                .execute("process-classes")
                .assertErrorFreeLog();

        File generated = new File(basedir, "target/generated-sources");
        File output = new File(generated, "thrift");

        assertTrue(new File(output, IDL_FILE).isFile());

        List<String> lines = readAllLines(output.toPath().resolve(IDL_FILE));
        assertTrue(lines.contains("enum ResultCode {"));
        assertTrue(lines.contains("struct LogEntry {"));
        assertTrue(lines.contains("service Scribe {"));
    }
}
