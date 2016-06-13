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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.io.File;
import java.net.URI;
import java.util.EnumSet;
import java.util.Set;

public class SwiftGeneratorConfig
{
    private final URI inputBase;
    private final Iterable<URI> includeSearchPaths;
    private final File outputFolder;
    private final String overridePackage;
    private final String defaultPackage;
    private final Set<SwiftGeneratorTweak> generatorTweaks;
    private final boolean generateIncludedCode;
    private final String codeFlavor;

    private SwiftGeneratorConfig(
            final URI inputBase,
            Iterable<URI> includeSearchPaths, final File outputFolder,
            final String overridePackage,
            final String defaultPackage,
            final Set<SwiftGeneratorTweak> generatorTweaks,
            final boolean generateIncludedCode,
            final String codeFlavor)
    {
        this.inputBase = inputBase;
        this.includeSearchPaths = includeSearchPaths;
        this.outputFolder = outputFolder;
        this.overridePackage = overridePackage;
        this.defaultPackage = defaultPackage;
        this.generatorTweaks = generatorTweaks;
        this.generateIncludedCode = generateIncludedCode;
        this.codeFlavor = codeFlavor;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Returns the input base URI to load Thrift IDL files.
     */
    public URI getInputBase()
    {
        return inputBase;
    }

    /**
     * Returns the list of URIs used as prefixes to search for include files.
     */
    public Iterable<URI> getIncludeSearchPaths()
    {
        return includeSearchPaths;
    }

    /**
     * Returns the output folder which will contain the generated sources.
     */
    public File getOutputFolder()
    {
        return outputFolder;
    }

    /**
     * If non-null, overrides the java namespace definitions in the IDL files.
     */
    public String getOverridePackage()
    {
        return overridePackage;
    }

    /**
     * If no namespace was set in the Thrift IDL file, fall back to this package.
     */
    public String getDefaultPackage()
    {
        return defaultPackage;
    }

    /**
     * Returns true if the tweak is set, false otherwise.
     */
    public boolean containsTweak(final SwiftGeneratorTweak tweak)
    {
        return generatorTweaks.contains(tweak);
    }

    /**
     * If true, generate code for all included Thrift IDLs instead of just referring to
     * them.
     */
    public boolean isGenerateIncludedCode()
    {
        return generateIncludedCode;
    }

    /**
     * The template to use for generating source code.
     */
    public String getCodeFlavor()
    {
        return codeFlavor;
    }

    public static class Builder
    {
        private URI inputBase = null;
        private Iterable<URI> includeSearchPaths = null;
        private File outputFolder = null;
        private String overridePackage = null;
        private String defaultPackage = null;
        private Set<SwiftGeneratorTweak> generatorTweaks = EnumSet.noneOf(SwiftGeneratorTweak.class);
        private boolean generateIncludedCode = false;
        private String codeFlavor = null;

        private Builder()
        {
        }

        public SwiftGeneratorConfig build()
        {
            Preconditions.checkState(outputFolder != null, "output folder must be set!");

            Preconditions.checkState(inputBase != null, "input base uri must be set to load includes!");
            Preconditions.checkState(codeFlavor != null, "no code flavor selected!");

            if (includeSearchPaths == null) {
                includeSearchPaths = Lists.newArrayList();
            }

            return new SwiftGeneratorConfig(
                    inputBase,
                    includeSearchPaths,
                    outputFolder,
                    overridePackage,
                    defaultPackage,
                    generatorTweaks,
                    generateIncludedCode,
                    codeFlavor);
        }

        public Builder inputBase(final URI inputBase)
        {
            this.inputBase = inputBase;
            return this;
        }

        public Builder includeSearchPaths(final Iterable<URI> includeSearchPaths)
        {
            this.includeSearchPaths = includeSearchPaths;
            return this;
        }

        public Builder outputFolder(final File outputFolder)
        {
            this.outputFolder = outputFolder;
            return this;
        }

        public Builder overridePackage(final String overridePackage)
        {
            this.overridePackage = overridePackage;
            return this;
        }

        public Builder defaultPackage(final String defaultPackage)
        {
            this.defaultPackage = defaultPackage;
            return this;
        }

        public Builder addTweak(final SwiftGeneratorTweak tweak)
        {
            this.generatorTweaks.add(tweak);
            return this;
        }

        public Builder generateIncludedCode(final boolean generateIncludedCode)
        {
            this.generateIncludedCode = generateIncludedCode;
            return this;
        }

        public Builder codeFlavor(final String codeFlavor)
        {
            this.codeFlavor = codeFlavor;
            return this;
        }
    }
}
