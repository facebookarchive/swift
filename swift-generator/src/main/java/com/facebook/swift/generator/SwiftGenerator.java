package com.facebook.swift.generator;

import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.ThriftIdlParser;
import com.facebook.swift.parser.model.Definition;
import com.facebook.swift.parser.model.Document;
import com.facebook.swift.parser.model.Header;
import com.facebook.swift.parser.model.Service;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import org.antlr.stringtemplate.StringTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Parses a Thrift IDL file and writes out initial annotated java classes.
 */
public class SwiftGenerator
{
    private static final Logger LOG = LoggerFactory.getLogger(SwiftGenerator.class);

    public static void main(final String ... args) throws Exception
    {
        final SwiftGenerator generator = new SwiftGenerator("/Users/hgschmie/fb/src/swift/swift-idl-parser/src/test/resources/fb303.thrift");
        generator.run();
    }

    private final File thriftFile;
    private final TemplateLoader templateLoader;

    SwiftGenerator(final String thriftFileName)
    {
        this.thriftFile = new File(thriftFileName);
        this.templateLoader = new TemplateLoader("java/regular.st");
    }

    public void run() throws Exception
    {
        final Document document = ThriftIdlParser.parseThriftIdl(Files.newReaderSupplier(thriftFile, Charsets.UTF_8));

        final Header header = document.getHeader();
        final String javaNamespace = header.getNamespace("java");
        Preconditions.checkState(!StringUtils.isEmpty(javaNamespace), "thrift file %s does not declare a java namespace!", thriftFile.getAbsolutePath());

        for (final Definition definition : document.getDefinitions()) {
            if (definition instanceof Service) {
                final StringTemplate interfaceTemplate = templateLoader.load("interface");
                interfaceTemplate.setAttribute("namespace", javaNamespace);
                interfaceTemplate.setAttribute("name", ((Service) definition).getName());
                LOG.info(interfaceTemplate.toString());
            }
        }
    }
}


