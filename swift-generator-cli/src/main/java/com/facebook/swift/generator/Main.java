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
import com.google.common.collect.Lists;

import java.io.File;
import java.net.URI;

import javax.annotation.Nonnull;

public class Main
{
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

        SwiftGeneratorConfig.Builder configBuilder = SwiftGeneratorConfig.builder()
                .inputBase(workingDirectory)
                .addInputs(Lists.transform(cliConfig.inputFiles, new Function<File, URI>()
                {
                    @Nonnull
                    @Override
                    public URI apply(@Nonnull File input)
                    {
                        return input.toURI();
                    }
                }))
                .outputFolder(cliConfig.outputDirectory)
                .overridePackage(cliConfig.overridePackage)
                .defaultPackage(cliConfig.defaultPackage)
                .generateIncludedCode(cliConfig.generateIncludedCode)
                .codeFlavor(cliConfig.generateBeans ? "java-regular" : "java-immutable");

        for (SwiftGeneratorTweak tweak : cliConfig.tweaks) {
            configBuilder.addTweak(tweak);
        }

        if (cliConfig.addThriftExceptions) {
            configBuilder.addTweak(SwiftGeneratorTweak.ADD_THRIFT_EXCEPTION);
        }

        new SwiftGenerator(configBuilder.build()).parse();
    }
}
