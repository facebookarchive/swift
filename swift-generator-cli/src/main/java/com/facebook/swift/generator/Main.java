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

import com.beust.jcommander.JCommander;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.net.URI;

public class Main
{

    public static final Function<File,URI> FILE_TO_URI_TRANSFORM = new Function<File, URI>()
    {
        @Nonnull
        @Override
        public URI apply(@Nonnull File input)
        {
            return input.toURI();
        }
    };

    public static void main(final String ... args) throws Exception
    {
        URI workingDirectory = new File(System.getProperty("user.dir")).getCanonicalFile().toURI();

        SwiftGeneratorCommandLineConfig cliConfig = new SwiftGeneratorCommandLineConfig();
        JCommander jCommander = new JCommander(cliConfig, args);
        jCommander.setProgramName(SwiftGenerator.class.getSimpleName());

        if (cliConfig.inputFiles == null) {
            jCommander.usage();
            return;
        }

        Iterable<URI> includeSearchPaths =
                Iterables.transform(cliConfig.includePaths, FILE_TO_URI_TRANSFORM);

        SwiftGeneratorConfig.Builder configBuilder = SwiftGeneratorConfig.builder()
                .inputBase(workingDirectory)
                .includeSearchPaths(includeSearchPaths)
                .outputFolder(cliConfig.outputDirectory)
                .overridePackage(cliConfig.overridePackage)
                .defaultPackage(cliConfig.defaultPackage)
                .generateIncludedCode(cliConfig.generateIncludedCode)
                .codeFlavor(cliConfig.generateBeans ? "java-regular" : "java-immutable");

        for (SwiftGeneratorTweak tweak : cliConfig.tweaks) {
            configBuilder.addTweak(tweak);
        }

        if (cliConfig.usePlainJavaNamespace) {
            configBuilder.addTweak(SwiftGeneratorTweak.USE_PLAIN_JAVA_NAMESPACE);
        }

        if (cliConfig.fallbackToPlainJavaNamespace) {
          configBuilder.addTweak(SwiftGeneratorTweak.FALLBACK_TO_PLAIN_JAVA_NAMESPACE);
        }

      Iterable<URI> inputs = Iterables.transform(cliConfig.inputFiles, FILE_TO_URI_TRANSFORM);

        new SwiftGenerator(configBuilder.build()).parse(inputs);
    }
}
