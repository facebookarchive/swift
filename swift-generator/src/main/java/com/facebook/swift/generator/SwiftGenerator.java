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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Parses a Thrift IDL file and writes out initial annotated java classes.
 */
public class SwiftGenerator
{
    private static final String THRIFT_FOLDER = System.getProperty("user.home") + "/fb/src/swift/swift-idl-parser/src/test/resources";
    private static final String OUTPUT_FOLDER = System.getProperty("user.home") + "/fb/workspace/default/Demo/src/";

    public static void main(final String ... args) throws Exception
    {
        final SwiftGeneratorConfig config = new SwiftGeneratorConfig(new File(THRIFT_FOLDER),
                                                                     new File [] { new File(THRIFT_FOLDER, "hive/metastore.thrift"),
                                                                                   new File(THRIFT_FOLDER, "fb303.thrift") },
                                                                     new File(OUTPUT_FOLDER),
                                                                     "com.fb.test");
        final SwiftGenerator generator = new SwiftGenerator(config);
        generator.parse();
    }

    private final File outputFolder;
    private final SwiftGeneratorConfig swiftGeneratorConfig;

    private final TemplateLoader templateLoader;

    public SwiftGenerator(final SwiftGeneratorConfig swiftGeneratorConfig)
    {
        this.swiftGeneratorConfig = swiftGeneratorConfig;

        this.outputFolder = swiftGeneratorConfig.getOutputFolder();
        if (outputFolder != null) {
            outputFolder.mkdirs();
        }

        this.templateLoader = new TemplateLoader("java/regular.st");
    }

    public void parse() throws Exception
    {
        final List<SwiftDocumentContext> contexts = Lists.newArrayList();
        for (final File file : swiftGeneratorConfig.getInputFiles()) {
            contexts.add(parseDocument(file, new TypeRegistry()));
        }

        for (final SwiftDocumentContext context : contexts) {
            generateFiles(context);
        }
    }

    private SwiftDocumentContext parseDocument(final File thriftFile,
                                               final TypeRegistry typeRegistry) throws IOException
    {
        final String thriftName = thriftFile.getName();
        final int idx = thriftName.lastIndexOf('.');
        final String thriftNamespace = (idx == -1) ? thriftName : thriftName.substring(0, idx);

        Preconditions.checkState(thriftFile.exists(), "The file %s does not exist!", thriftFile.getAbsolutePath());
        Preconditions.checkState(thriftFile.canRead(), "The file %s can not be read!", thriftFile.getAbsolutePath());
        Preconditions.checkState(!StringUtils.isEmpty(thriftNamespace), "The file %s can not be translated to a namespace", thriftFile.getAbsolutePath());
        final SwiftDocumentContext context = new SwiftDocumentContext(thriftFile, thriftNamespace, typeRegistry);

        final Document document = context.getDocument();
        final Header header = document.getHeader();
        final String javaNamespace = Objects.firstNonNull(swiftGeneratorConfig.getDefaultPackage(), header.getNamespace("java"));
        Preconditions.checkState(!StringUtils.isEmpty(javaNamespace), "thrift file %s does not declare a java namespace!", thriftFile.getAbsolutePath());

        for (final String include : header.getIncludes()) {
            parseDocument(new File(swiftGeneratorConfig.getInputFolder(), include), typeRegistry);
        }

        document.visit(new TypeVisitor(javaNamespace, context));

        return context;
    }

    private void generateFiles(final SwiftDocumentContext context) throws IOException
    {
        Preconditions.checkState(outputFolder != null, "The output folder was not set!");
        Preconditions.checkState(outputFolder.isDirectory() && outputFolder.canWrite() && outputFolder.canExecute(), "output folder '%s' is not valid!", outputFolder.getAbsolutePath());

        final List<DocumentVisitor> visitors = Lists.newArrayList();
        visitors.add(new ServiceVisitor(templateLoader, context, outputFolder));
        visitors.add(new StructVisitor(templateLoader, context, outputFolder));
        visitors.add(new ExceptionVisitor(templateLoader, context, outputFolder));
        visitors.add(new IntegerEnumVisitor(templateLoader, context, outputFolder));
        visitors.add(new StringEnumVisitor(templateLoader, context, outputFolder));

        for (DocumentVisitor visitor : visitors) {
            context.getDocument().visit(visitor);
        }
    }
}
