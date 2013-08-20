/*
 * Copyright (C) 2013 Facebook, Inc.
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
package com.facebook.swift.generator.swift2thrift;

import com.beust.jcommander.Parameter;

import java.io.File;
import java.util.List;

public class Swift2ThriftGeneratorCommandLineConfig {
    @Parameter(description = "Swift Java source files")
    public List<String> inputFiles;

    @Parameter(names = "-out", description = "Thrift IDL output file, defaults to stdout")
    public File outputFile;

    @Parameter(names = "-map", arity = 2, description = "Map of external type or service to include file")
    public List<String> includeMap;

    @Parameter(names = {"-v", "-verbose"}, description = "Show verbose messages")
    public boolean verbose = false;

    @Parameter(names = {"-package", "-default_package"}, description = "Default package for unqualified classes")
    public String defaultPackage = "";

    @Parameter(names = "-namespace", arity = 2, description = "Namespace for a particular language to include")
    public List<String> namespaceMap;
}
