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

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableMap;

import java.util.Iterator;

public class Main
{
    public static void main(final String ... args) throws Exception
    {
        Swift2ThriftGeneratorCommandLineConfig cliConfig = new Swift2ThriftGeneratorCommandLineConfig();
        JCommander jCommander = new JCommander(cliConfig, args);
        jCommander.setProgramName(Swift2ThriftGenerator.class.getSimpleName());

        if (cliConfig.inputFiles == null) {
            jCommander.usage();
            return;
        }

        ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
        if (cliConfig.includeMap != null) {
            Iterator<String> iter = cliConfig.includeMap.iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                String value = iter.next();
                mapBuilder.put(key, value);
            }
        }
        ImmutableMap.Builder<String, String> namespaceMapBuilder = ImmutableMap.builder();
        if (cliConfig.namespaceMap != null) {
            Iterator<String> iter = cliConfig.namespaceMap.iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                String value = iter.next();
                namespaceMapBuilder.put(key, value);
            }
        }

        Swift2ThriftGeneratorConfig.Builder configBuilder = Swift2ThriftGeneratorConfig.builder()
                .outputFile(cliConfig.outputFile)
                .includeMap(mapBuilder.build())
                .verbose(cliConfig.verbose)
                .defaultPackage(cliConfig.defaultPackage)
                .namespaceMap(namespaceMapBuilder.build())
                .allowMultiplePackages(cliConfig.allowMultiplePackages)
                .recursive(cliConfig.recursive);

        new Swift2ThriftGenerator(configBuilder.build()).parse(cliConfig.inputFiles);
    }
}
