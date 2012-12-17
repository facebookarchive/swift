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

import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.generator.visitors.ExceptionVisitor;
import com.facebook.swift.generator.visitors.IntegerEnumVisitor;
import com.facebook.swift.generator.visitors.ServiceVisitor;
import com.facebook.swift.generator.visitors.StringEnumVisitor;
import com.facebook.swift.generator.visitors.StructVisitor;
import com.facebook.swift.generator.visitors.TypeVisitor;
import com.facebook.swift.parser.model.Document;
import com.facebook.swift.parser.model.Header;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.facebook.swift.generator.util.SwiftInternalStringUtils.isBlank;

/**
 * Parses a Thrift IDL file and writes out initial annotated java classes.
 */
public class SwiftGenerator
{
    private static final Logger LOG = LoggerFactory.getLogger(SwiftGenerator.class);

    private static final Map<String, String> TEMPLATES = ImmutableMap.of("java-regular", "java/regular.st",
                                                                         "java-immutable", "java/immutable.st");
    public static void main(final String ... args) throws Exception
    {
        Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if (rootLogger instanceof SimpleLogger) {
            SimpleLogger logbackLogger = (SimpleLogger) rootLogger;
        }

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");

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
                .addInputs(Lists.transform(cliConfig.inputFiles, new Function<File, URI>() {
                    @Nullable
                    @Override
                    public URI apply(@Nullable File input)
                    {
                        return input.toURI();
                    }
                }))
                .outputFolder(cliConfig.outputDirectory)
                .overridePackage(cliConfig.overridePackage)
                .defaultPackage(cliConfig.defaultPackage)
                .addTweak(SwiftGeneratorTweak.ADD_CLOSEABLE_INTERFACE)
                .generateIncludedCode(cliConfig.generateIncludedCode)
                .codeFlavor(cliConfig.mutableTypes ? "java-regular" : "java-immutable");

        if (cliConfig.addThriftExceptions) {
            configBuilder.addTweak(SwiftGeneratorTweak.ADD_THRIFT_EXCEPTION);
        }

        new SwiftGenerator(configBuilder.build()).parse();
    }

    private final File outputFolder;
    private final SwiftGeneratorConfig swiftGeneratorConfig;

    private final TemplateLoader templateLoader;

    public SwiftGenerator(final SwiftGeneratorConfig swiftGeneratorConfig)
    {
        Preconditions.checkState(TEMPLATES.get(swiftGeneratorConfig.getCodeFlavor()) != null, "Templating type %s is unknown!", swiftGeneratorConfig.getCodeFlavor());

        this.swiftGeneratorConfig = swiftGeneratorConfig;

        this.outputFolder = swiftGeneratorConfig.getOutputFolder();
        if (outputFolder != null) {
            outputFolder.mkdirs();
        }

        LOG.debug("Writing source files into {} using {} ...", outputFolder, swiftGeneratorConfig.getCodeFlavor());

        this.templateLoader = new TemplateLoader(TEMPLATES.get(swiftGeneratorConfig.getCodeFlavor()));
    }

    public void parse() throws Exception
    {
        Iterable<URI> inputFiles = swiftGeneratorConfig.getInputs();
        if (inputFiles == null) {
            LOG.error("No input files!");
            return;
        }

        LOG.info("Parsing Thrift IDL from {}...", swiftGeneratorConfig.getInputs());

        final Map<String, SwiftDocumentContext> contexts = Maps.newHashMap();
        for (final URI inputUri : swiftGeneratorConfig.getInputs()) {

            parseDocument(inputUri.isAbsolute() ? inputUri
                                                : swiftGeneratorConfig.getInputBase().resolve(inputUri),
                          contexts,
                          new TypeRegistry(),
                          new TypedefRegistry());
        }

        LOG.info("IDL parsing complete, writing java code...");

        for (final SwiftDocumentContext context : contexts.values()) {
            generateFiles(context);
        }

        LOG.info("Java code generation complete.");
    }

    private void parseDocument(final URI thriftUri,
                               @Nullable final Map<String, SwiftDocumentContext> contexts,
                               final TypeRegistry typeRegistry,
                               final TypedefRegistry typedefRegistry) throws IOException
    {
        Preconditions.checkState(thriftUri != null && thriftUri.isAbsolute() && !thriftUri.isOpaque(), "Only absolute, non opaque URIs can be parsed!");
        LOG.debug("Parsing {}...", thriftUri);

        final String thriftNamespace = extractThriftNamespace(thriftUri);

        Preconditions.checkState(!isBlank(thriftNamespace), "Thrift URI %s can not be translated to a namespace", thriftUri);
        final SwiftDocumentContext context = new SwiftDocumentContext(thriftUri, thriftNamespace, swiftGeneratorConfig, typeRegistry, typedefRegistry);

        final Document document = context.getDocument();
        final Header header = document.getHeader();
        final String javaNamespace = Objects.firstNonNull(Objects.firstNonNull(swiftGeneratorConfig.getOverridePackage(),
                                                                               header.getNamespace("java")),
                                                          swiftGeneratorConfig.getDefaultPackage());
        Preconditions.checkState(!isBlank(javaNamespace), "thrift uri %s does not declare a java namespace!", thriftUri);

        for (final String include : header.getIncludes()) {
            final URI includeUri = swiftGeneratorConfig.getInputBase().resolve(include);
            LOG.debug("Found {} included from {}.", includeUri, thriftUri);
            parseDocument(includeUri,
                          // If the includes should also generate code, pass the list of
                          // contexts down to the include parser, otherwise pass a null in
                          swiftGeneratorConfig.isGenerateIncludedCode() ? contexts : null,
                          typeRegistry,
                          typedefRegistry);
        }

        document.visit(new TypeVisitor(javaNamespace, context));

        if (contexts != null && contexts.put(context.getNamespace(), context) != null) {
            LOG.info("Thrift Namespace {} included multiple times!", context.getNamespace());
        }
    }

    private String extractThriftNamespace(final URI thriftUri)
    {
        final String path = thriftUri.getPath();
        final String filename = Iterables.getLast(Splitter.on('/').split(path), null);
        Preconditions.checkState(filename != null, "No thrift namespace found in %s", thriftUri);

        final String name = Iterables.getFirst(Splitter.on('.').split(filename), null);
        Preconditions.checkState(name != null, "No thrift namespace found in %s", thriftUri);
        return name;
    }

    private void generateFiles(final SwiftDocumentContext context) throws IOException
    {
        LOG.debug("Generating code for {}...", context.getNamespace());

        Preconditions.checkState(outputFolder != null, "The output folder was not set!");
        Preconditions.checkState(outputFolder.isDirectory() && outputFolder.canWrite() && outputFolder.canExecute(), "output folder '%s' is not valid!", outputFolder.getAbsolutePath());

        final List<DocumentVisitor> visitors = Lists.newArrayList();
        visitors.add(new ServiceVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));
        visitors.add(new StructVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));
        visitors.add(new ExceptionVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));
        visitors.add(new IntegerEnumVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));
        visitors.add(new StringEnumVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));

        for (DocumentVisitor visitor : visitors) {
            context.getDocument().visit(visitor);
        }
    }
}
