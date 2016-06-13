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

import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.generator.visitors.ConstantsVisitor;
import com.facebook.swift.generator.visitors.ExceptionVisitor;
import com.facebook.swift.generator.visitors.IntegerEnumVisitor;
import com.facebook.swift.generator.visitors.ServiceVisitor;
import com.facebook.swift.generator.visitors.StringEnumVisitor;
import com.facebook.swift.generator.visitors.StructVisitor;
import com.facebook.swift.generator.visitors.TypeVisitor;
import com.facebook.swift.generator.visitors.UnionVisitor;
import com.facebook.swift.parser.model.Document;
import com.facebook.swift.parser.model.Header;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import io.airlift.log.Logger;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static com.facebook.swift.generator.util.SwiftInternalStringUtils.isBlank;

/**
 * Parses a Thrift IDL file and writes out initial annotated java classes.
 */
public class SwiftGenerator
{
    private static final Logger LOG = Logger.get(SwiftGenerator.class);

    private static final Map<String, ImmutableList<String>> TEMPLATES =
            ImmutableMap.of(
                    "java-regular", ImmutableList.of("java/common.st", "java/regular.st"),
                    "java-immutable", ImmutableList.of("java/common.st", "java/immutable.st"),
                    "java-ctor", ImmutableList.of("java/common.st", "java/ctor.st")
            );

    private final File outputFolder;
    private final SwiftGeneratorConfig swiftGeneratorConfig;
    private final TemplateLoader templateLoader;
    private final Set<URI> parsedDocuments = new HashSet<>();
    private final Stack<URI> parentDocuments = new Stack<>();

    public SwiftGenerator(final SwiftGeneratorConfig swiftGeneratorConfig)
    {
        Preconditions.checkState(TEMPLATES.get(swiftGeneratorConfig.getCodeFlavor()) != null, "Templating type %s is unknown!", swiftGeneratorConfig.getCodeFlavor());

        this.swiftGeneratorConfig = swiftGeneratorConfig;

        this.outputFolder = swiftGeneratorConfig.getOutputFolder();
        if (outputFolder != null) {
            outputFolder.mkdirs();
        }

        LOG.debug("Writing source files into %s using %s ...", outputFolder, swiftGeneratorConfig.getCodeFlavor());

        this.templateLoader = new TemplateLoader(TEMPLATES.get(swiftGeneratorConfig.getCodeFlavor()));
    }

    public void parse(Iterable<URI> inputs) throws Exception
    {
        Preconditions.checkArgument(
                inputs != null && inputs.iterator().hasNext(),
                "No input files!");

        LOG.info("Parsing Thrift IDL from %s...", inputs);

        final Map<String, SwiftDocumentContext> contexts = Maps.newHashMap();
        for (final URI inputUri : inputs) {
            parsedDocuments.clear();
            parseDocument(
                    inputUri.isAbsolute() ? inputUri : swiftGeneratorConfig.getInputBase().resolve(inputUri),
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
        Preconditions.checkArgument(
                !parentDocuments.contains(thriftUri),
                "Input %s recursively includes itself (%s)", thriftUri, Joiner.on(" -> ").join(parentDocuments) + " -> " + thriftUri);

        if (parsedDocuments.contains(thriftUri)) {
            LOG.debug("Skipping already parsed file %s...", thriftUri);
            return;
        }

        LOG.debug("Parsing %s...", thriftUri);

        final String thriftNamespace = extractThriftNamespace(thriftUri);

        Preconditions.checkState(!isBlank(thriftNamespace), "Thrift URI %s can not be translated to a namespace", thriftUri);
        final SwiftDocumentContext context = new SwiftDocumentContext(thriftUri, thriftNamespace, swiftGeneratorConfig, typeRegistry, typedefRegistry);

        final Document document = context.getDocument();
        final Header header = document.getHeader();

        String javaPackage = context.getJavaPackage();

        // Add a Constants type so that the Constants visitor can render is.
        typeRegistry.add(new SwiftJavaType(thriftNamespace, "Constants", "Constants", javaPackage));

        // Make a note that this document is a parent of all the documents included, directly or recursively
        parentDocuments.push(thriftUri);

        try {
            for (final String include : header.getIncludes()) {
                URI foundIncludeUri = null;

                for (URI includeSearchPath : swiftGeneratorConfig.getIncludeSearchPaths()) {
                    if (includeSearchPath.isAbsolute()) {
                        URI candidateIncludeUri = includeSearchPath.resolve(include);
                        if (uriExists(candidateIncludeUri)) {
                            foundIncludeUri = candidateIncludeUri;
                            break;
                        }
                    }
                    else {
                        URI candidateIncludeUri = swiftGeneratorConfig.getInputBase()
                                                                      .resolve(includeSearchPath)
                                                                      .resolve(include);
                        if (uriExists(candidateIncludeUri)) {
                            foundIncludeUri = candidateIncludeUri;
                            break;
                        }
                    }
                }

                if (foundIncludeUri == null) {
                    foundIncludeUri = swiftGeneratorConfig.getInputBase().resolve(include);
                }

                LOG.debug("Found %s included from %s.", foundIncludeUri, thriftUri);
                parseDocument(foundIncludeUri,
                              // If the includes should also generate code, pass the list of
                              // contexts down to the include parser, otherwise pass a null in
                              swiftGeneratorConfig.isGenerateIncludedCode() ? contexts : null,
                              typeRegistry,
                              typedefRegistry);
            }
        }
        finally {
            // Done parsing this document's includes, remove it from the parent chain
            parentDocuments.pop();
        }

        // Make a note that we've already parsed this document
        parsedDocuments.add(thriftUri);

        document.visit(new TypeVisitor(javaPackage, context));

        if (contexts != null && contexts.put(context.getNamespace(), context) != null) {
            LOG.info("Thrift Namespace %s included multiple times!", context.getNamespace());
        }
    }

    private boolean uriExists(final URI uri)
    {
        try (InputStream stream = Resources.asByteSource(uri.toURL()).openStream()) {
            stream.close();
            return true;
        }
        catch (IOException e) {
            return false;
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
        LOG.debug("Generating code for %s...", context.getNamespace());

        Preconditions.checkState(outputFolder != null, "The output folder was not set!");
        Preconditions.checkState(outputFolder.isDirectory() && outputFolder.canWrite() && outputFolder.canExecute(), "output folder '%s' is not valid!", outputFolder.getAbsolutePath());

        final List<DocumentVisitor> visitors = Lists.newArrayList();
        visitors.add(new ServiceVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));
        visitors.add(new StructVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));
        visitors.add(new UnionVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));
        visitors.add(new ExceptionVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));
        visitors.add(new IntegerEnumVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));
        visitors.add(new StringEnumVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));
        visitors.add(new ConstantsVisitor(templateLoader, context, swiftGeneratorConfig, outputFolder));

        for (DocumentVisitor visitor : visitors) {
            context.getDocument().visit(visitor);
            visitor.finish();
        }
    }
}
