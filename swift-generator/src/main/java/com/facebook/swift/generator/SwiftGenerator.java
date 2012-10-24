package com.facebook.swift.generator;

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
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Parses a Thrift IDL file and writes out initial annotated java classes.
 */
public class SwiftGenerator
{
    private static final String THRIFT_FOLDER = System.getProperty("user.home") + "/fb/src/swift/swift-idl-parser/src/test/resources";
    private static final String OUTPUT_FOLDER = System.getProperty("user.home") + "/fb/workspace/default/Demo/src/";

    public static void main(final String ... args) throws Exception
    {
        final SwiftGeneratorConfig config = new SwiftGeneratorConfig(THRIFT_FOLDER,
                                                                     new String [] { "hive/metastore.thrift", "fb303.thrift" },
                                                                     OUTPUT_FOLDER);
        final SwiftGenerator generator = new SwiftGenerator(config);
        generator.parse();
    }

    private final File outputFolder;
    private final SwiftGeneratorConfig swiftGeneratorConfig;

    private final TemplateLoader templateLoader;

    public SwiftGenerator(final SwiftGeneratorConfig swiftGeneratorConfig)
    {
        this.swiftGeneratorConfig = swiftGeneratorConfig;

        final String outputFolderName = swiftGeneratorConfig.getOutputFolderName();
        if (outputFolderName != null) {
            this.outputFolder = new File(outputFolderName);
            outputFolder.mkdirs();
        }
        else {
            outputFolder = null;
        }

        this.templateLoader = new TemplateLoader("java/regular.st");
    }

    public void parse() throws Exception
    {
        final List<SwiftDocumentContext> contexts = Lists.newArrayList();
        for (final String fileName : swiftGeneratorConfig.getInputFiles()) {
            contexts.add(parseDocument(fileName, new TypeRegistry()));
        }

        for (final SwiftDocumentContext context : contexts) {
            generateFiles(context);
        }
    }

    public SwiftDocumentContext parseDocument(final String fileName,
                                              final TypeRegistry typeRegistry) throws IOException
    {
        final String thriftName = new File(fileName).getName();
        final int idx = thriftName.lastIndexOf('.');
        final String thriftNamespace = (idx == -1) ? thriftName : thriftName.substring(0, idx);
        final File thriftFile = new File(swiftGeneratorConfig.getInputFolderName(), fileName);

        Preconditions.checkState(thriftFile.exists(), "The file %s does not exist!", thriftFile.getAbsolutePath());
        Preconditions.checkState(thriftFile.canRead(), "The file %s can not be read!", thriftFile.getAbsolutePath());
        Preconditions.checkState(!StringUtils.isEmpty(thriftNamespace), "The file %s can not be translated to a namespace", thriftFile.getAbsolutePath());
        final SwiftDocumentContext context = new SwiftDocumentContext(thriftFile, thriftNamespace, typeRegistry);

        final Document document = context.getDocument();
        final Header header = document.getHeader();
        final String javaNamespace = header.getNamespace("java");
        Preconditions.checkState(!StringUtils.isEmpty(javaNamespace), "thrift file %s does not declare a java namespace!", thriftFile.getAbsolutePath());

        for (final String include : header.getIncludes()) {
            parseDocument(include, typeRegistry);
        }

        document.visit(new TypeVisitor(javaNamespace, context));

        return context;
    }

    public void generateFiles(final SwiftDocumentContext context) throws IOException
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
