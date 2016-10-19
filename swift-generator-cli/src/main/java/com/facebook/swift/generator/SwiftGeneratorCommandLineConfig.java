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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;
import com.facebook.swift.generator.util.ColonParameterSplitter;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.List;
import java.util.Set;

public class SwiftGeneratorCommandLineConfig
{
    @Parameter(description = "Thrift IDL input files")
    public List<File> inputFiles;

    @Parameter(
            names = "-include_paths",
            description = "Colon-separated list of paths to search for include files",
            splitter = ColonParameterSplitter.class
    )
    public List<File> includePaths = Lists.newArrayList();

    @Parameter(names = "-out", description = "Output directory")
    public File outputDirectory = new File(System.getProperty("user.dir") + "/gen-swift");

    @Parameter(
            names = "-override_package",
            description = "Force generation of code in a specific package"
    )
    public String overridePackage = null;

    @Parameter(
            names = "-default_package",
            description = "Use this package if there is no package specified in the IDL for java"
    )
    public String defaultPackage = null;

    @Parameter(
            names = "-generate_included_files",
            description = "Generate code for included IDL files as well as specified IDL files"
    )
    public boolean generateIncludedCode = false;

    @Parameter(
            names = "-generate_beans",
            description = "Generate thrift types as mutable beans"
    )
    public boolean generateBeans = false;

    @Parameter(
            names = "-tweak",
            description = "Enable specific code generation tweaks"
    )
    public Set<SwiftGeneratorTweak> tweaks = Sets.newHashSet();

    @Parameter(
            names = "-use_java_namespace",
            description = "Use 'java' namespace instead of 'java.swift' namespace"
    )
    public boolean usePlainJavaNamespace = false;

  @Parameter(
      names = "-fallback_to_java_namespace",
      description = "Use 'java' namespace if 'java.swift' namespace is not present"
  )
  public boolean fallbackToPlainJavaNamespace = false;
}
