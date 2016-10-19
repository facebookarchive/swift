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
package com.facebook.mojo;

import com.facebook.swift.generator.SwiftGenerator;
import com.facebook.swift.generator.SwiftGeneratorConfig;
import com.facebook.swift.generator.SwiftGeneratorTweak;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import static java.lang.String.format;

/**
 * Process IDL files and generates source code from the IDL files.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class SwiftMojo extends AbstractMojo
{

    private static final Pattern scanRequiredPattern
            = Pattern.compile("^[/\\\\]|[*?]|\\.\\.|[/\\\\]$");

    /**
     * Skip the plugin execution.
     */
    @Parameter(defaultValue = "false")
    private boolean skip = false;

    /**
     * Override java package for the generated classes. If unset, the java namespace from
     * the IDL files is used. If a value is set here, the java package definition from
     * the IDL files is ignored.
     */
    @Parameter
    private String overridePackage = null;

    /**
     * Give a default Java package for generated classes if the IDL files do not contain
     * a java namespace definition. This package is only used if the IDL files do not
     * contain a java namespace definition.
     */
    @Parameter
    private String defaultPackage = null;

    /**
     * IDL files to process.
     */
    @Parameter(required = true)
    private FileSet idlFiles = null;

    /**
     * Set the Output folder for generated code.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/swift", required = true)
    private File outputFolder = null;

    /**
     * Generate code for included IDL files. If true, generate Java code for all IDL
     * files that are listed in the idlFiles set and all IDL files loaded through include
     * statements. Default is false (generate only code for explicitly listed IDL files).
     */
    @Parameter(defaultValue = "false")
    private boolean generateIncludedCode = false;

    /**
     * Add {@link org.apache.thrift.TException} to each method signature. This exception
     * is thrown when a thrift internal error occurs.
     */
    @Parameter(defaultValue = "true")
    private boolean addThriftExceptions = true;

    /**
     * Have generated services extend {@link Closeable} and a close method.
     */
    @Parameter(defaultValue = "false")
    private boolean addCloseableInterface = false;

    /**
     * Generated exceptions extends {@link RuntimeException}, not {@link Exception}.
     */
    @Parameter(defaultValue = "true")
    private boolean extendRuntimeException = true;

    /**
     * Select the flavor of the generated source code. Default is "java-regular".
     */
    @Parameter(defaultValue = "java-regular")
    private String codeFlavor = "java-regular";

    /**
     * Use the 'java' namespace instead of the 'java.swift' namespace.
     */
    @Parameter(defaultValue = "false")
    private boolean usePlainJavaNamespace = false;

    /**
     * Use the 'java' namespace if 'java.swift' namespace is not present.
     */
    @Parameter(defaultValue = "false")
    private boolean fallbackToPlainJavaNamespace = false;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project = null;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            if (!skip)
            {

                final File inputFolder = new File(idlFiles.getDirectory());

                final List<File> files = getFiles(inputFolder);

                final SwiftGeneratorConfig.Builder configBuilder = SwiftGeneratorConfig.builder()
                    .inputBase(inputFolder.toURI())
                    .outputFolder(outputFolder)
                    .overridePackage(overridePackage)
                    .defaultPackage(defaultPackage)
                    .generateIncludedCode(generateIncludedCode)
                    .codeFlavor(codeFlavor);

                if (usePlainJavaNamespace)
                {
                    configBuilder.addTweak(SwiftGeneratorTweak.USE_PLAIN_JAVA_NAMESPACE);
                }

                if (fallbackToPlainJavaNamespace)
                {
                    configBuilder.addTweak(SwiftGeneratorTweak.FALLBACK_TO_PLAIN_JAVA_NAMESPACE);
                }

                if (addThriftExceptions)
                {
                    configBuilder.addTweak(SwiftGeneratorTweak.ADD_THRIFT_EXCEPTION);
                }

                if (addCloseableInterface)
                {
                    configBuilder.addTweak(SwiftGeneratorTweak.ADD_CLOSEABLE_INTERFACE);
                }

                if (extendRuntimeException)
                {
                    configBuilder.addTweak(SwiftGeneratorTweak.EXTEND_RUNTIME_EXCEPTION);
                }

                final SwiftGenerator generator = new SwiftGenerator(configBuilder.build());
                generator.parse(files.stream().map(File::toURI).collect(toList()));

                project.addCompileSourceRoot(outputFolder.getPath());
            }
        } 
        catch (Exception e)
        {
            Throwables.propagateIfInstanceOf(e, MojoExecutionException.class);
            Throwables.propagateIfInstanceOf(e, MojoFailureException.class);

            getLog().error(format("While executing Mojo %s", this.getClass().getSimpleName()), e);
            throw new MojoExecutionException("Failure:", e);
        }
    }

    private static boolean requiresScan(String pattern)
    {
        Matcher matcher = scanRequiredPattern.matcher(pattern);
        return matcher.find();
    }

    @VisibleForTesting
    static boolean canBypassScan(
            List<String> includedFiles,
            List<String> excludedFiles)
    {
        // The directory scan can be bypassed, IFF
        // 1) There are no excludes
        // 2) There is at least one include string
        // 3) In the include strings, the following apply
        //    a) No * or ? in the string
        //    b) Doesn't end in / or \ (as that auto-adds ** to the end)
        //    c) Doesn't start with / or \ (as that has special matching rules)
        //    d) Doesn't have relative paths (
        return excludedFiles.isEmpty()
                && !includedFiles.isEmpty()
                && includedFiles
                .stream()
                .noneMatch(s -> requiresScan(s));
    }

    @SuppressWarnings("unchecked")
    private List<File> getFiles(File inputFolder) throws IOException
    {
        // On large source trees under the input folder, the directory
        // search can take a very long time. 
        if (canBypassScan(
                idlFiles.getIncludes(),
                idlFiles.getExcludes()))
        {
            return idlFiles.getIncludes()
                    .stream()
                    .map(s -> new File(inputFolder, s))
                    .collect(Collectors.toList());
        }

        return FileUtils.getFiles(inputFolder,
                                  Joiner.on(',').join(idlFiles.getIncludes()),
                                  Joiner.on(',').join(idlFiles.getExcludes()));
    }
}
