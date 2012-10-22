package com.facebook.swift.generator;

import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.generator.visitors.ExceptionVisitor;
import com.facebook.swift.generator.visitors.IntegerEnumVisitor;
import com.facebook.swift.generator.visitors.ServiceVisitor;
import com.facebook.swift.generator.visitors.StringEnumVisitor;
import com.facebook.swift.generator.visitors.StructVisitor;
import com.facebook.swift.generator.visitors.TypeVisitor;
import com.facebook.swift.parser.ThriftIdlParser;
import com.facebook.swift.parser.model.Document;
import com.facebook.swift.parser.model.Header;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Parses a Thrift IDL file and writes out initial annotated java classes.
 */
public class SwiftGenerator
{
    private static final String THRIFT_FOLDER = System.getProperty("user.home") + "/fb/src/swift/swift-idl-parser/src/test/resources";
    private static final String OUTPUT_FOLDER = "/tmp/output";

    private static final Logger LOG = LoggerFactory.getLogger(SwiftGenerator.class);

    public static void main(final String ... args) throws Exception
    {
        final SwiftGenerator generator = new SwiftGenerator(THRIFT_FOLDER, "hive/metastore.thrift", OUTPUT_FOLDER);
        generator.parse();
        generator.generate();
    }

    private final TypeRegistry typeRegistry;

    private final String thriftFolderName;
    private final String thriftNamespace;
    private final File thriftFile;

    private final File outputFolder;

    private final TemplateLoader templateLoader;
    private final Document document;

    public SwiftGenerator(final String thriftFolderName, final String thriftFileName, final String outputFolderName)
        throws IOException
    {
        this.thriftFolderName = thriftFolderName;
        final String thriftName = new File(thriftFileName).getName();
        final int idx = thriftName.lastIndexOf('.');
        this.thriftNamespace = (idx == -1) ? thriftName : thriftName.substring(0, idx);
        this.thriftFile = new File(thriftFolderName, thriftFileName);

        if (outputFolderName != null) {
            this.outputFolder = new File(outputFolderName);
            outputFolder.mkdirs();
        }
        else {
            outputFolder = null;
        }

        Preconditions.checkState(thriftFile.exists(), "The file %s does not exist!", thriftFile.getAbsolutePath());
        Preconditions.checkState(thriftFile.canRead(), "The file %s can not be read!", thriftFile.getAbsolutePath());
        Preconditions.checkState(!StringUtils.isEmpty(thriftNamespace), "The file %s can not be translated to a namespace", thriftFile.getAbsolutePath());

        typeRegistry = new TypeRegistry(thriftNamespace);

        this.templateLoader = new TemplateLoader("java/regular.st");
        this.document = ThriftIdlParser.parseThriftIdl(Files.newReaderSupplier(thriftFile, Charsets.UTF_8));

    }

    public void parse() throws Exception
    {
        final Header header = document.getHeader();
        final String javaNamespace = header.getNamespace("java");
        Preconditions.checkState(!StringUtils.isEmpty(javaNamespace), "thrift file %s does not declare a java namespace!", thriftFile.getAbsolutePath());

        for (final String include : header.getIncludes()) {
            final SwiftGenerator includeGen = new SwiftGenerator(thriftFolderName, include, null);
            includeGen.parse();
            typeRegistry.addAll(includeGen.getTypeRegistry());
        }

        final List<DocumentVisitor> visitors = Lists.newArrayList();
        visitors.add(new TypeVisitor(javaNamespace, typeRegistry));
        for (DocumentVisitor visitor : visitors) {
            document.visit(visitor);
        }
    }

    public void generate() throws IOException
    {
        Preconditions.checkState(outputFolder != null, "The output folder was not set!");
        Preconditions.checkState(outputFolder.isDirectory() && outputFolder.canWrite() && outputFolder.canExecute(), "output folder '%s' is not valid!", outputFolder.getAbsolutePath());

        final List<DocumentVisitor> visitors = Lists.newArrayList();
        visitors.add(new ServiceVisitor(templateLoader, typeRegistry, outputFolder));
        visitors.add(new StructVisitor(templateLoader, typeRegistry, outputFolder));
        visitors.add(new ExceptionVisitor(templateLoader, typeRegistry, outputFolder));
        visitors.add(new IntegerEnumVisitor(templateLoader, typeRegistry, outputFolder));
        visitors.add(new StringEnumVisitor(templateLoader, typeRegistry, outputFolder));

        for (DocumentVisitor visitor : visitors) {
            document.visit(visitor);
        }
    }

    private TypeRegistry getTypeRegistry()
    {
        return typeRegistry;
    }
}
