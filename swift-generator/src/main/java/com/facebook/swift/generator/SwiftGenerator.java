package com.facebook.swift.generator;

import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.generator.visitors.ServiceVisitor;
import com.facebook.swift.generator.visitors.TypeVisitor;
import com.facebook.swift.parser.ThriftIdlParser;
import com.facebook.swift.parser.model.Document;
import com.facebook.swift.parser.model.Header;
import com.facebook.swift.parser.model.Service;
import com.facebook.swift.parser.model.ThriftField;
import com.facebook.swift.parser.model.ThriftMethod;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.antlr.stringtemplate.StringTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses a Thrift IDL file and writes out initial annotated java classes.
 */
public class SwiftGenerator
{
    private static final String THRIFT_FOLDER = "/Users/hgschmie/fb/src/swift/swift-idl-parser/src/test/resources/hive";

    private static final Logger LOG = LoggerFactory.getLogger(SwiftGenerator.class);

    public static void main(final String ... args) throws Exception
    {
        final SwiftGenerator generator = new SwiftGenerator(THRIFT_FOLDER, "metastore.thrift");
        generator.parse();
        generator.generate();
    }

    private final TypeRegistry typeRegistry;
    final Map<String, DocumentVisitor> visitors = Maps.newLinkedHashMap();

    private final String thriftFolderName;
    private final String thriftNamespace;
    private final File thriftFile;

    private final TemplateLoader templateLoader;

    SwiftGenerator(final String thriftFolderName, final String thriftFileName)
    {
        this.thriftFolderName = thriftFolderName;
        final String thriftName = new File(thriftFileName).getName();
        final int idx = thriftName.lastIndexOf('.');
        this.thriftNamespace = (idx == -1) ? thriftName : thriftName.substring(0, idx);
        this.thriftFile = new File(thriftFolderName, thriftFileName);

        Preconditions.checkState(thriftFile.exists(), "The file %s does not exist!", thriftFile.getAbsolutePath());
        Preconditions.checkState(thriftFile.canRead(), "The file %s can not be read!", thriftFile.getAbsolutePath());
        Preconditions.checkState(!StringUtils.isEmpty(thriftNamespace), "The file %s can not be translated to a namespace", thriftFile.getAbsolutePath());

        typeRegistry = new TypeRegistry(thriftNamespace);

        this.templateLoader = new TemplateLoader("java/regular.st");
    }

    public void parse() throws Exception
    {
        final Document document = ThriftIdlParser.parseThriftIdl(Files.newReaderSupplier(thriftFile, Charsets.UTF_8));

        final Header header = document.getHeader();
        final String javaNamespace = header.getNamespace("java");
        Preconditions.checkState(!StringUtils.isEmpty(javaNamespace), "thrift file %s does not declare a java namespace!", thriftFile.getAbsolutePath());

        for (final String include : header.getIncludes()) {
            final SwiftGenerator includeGen = new SwiftGenerator(thriftFolderName, include);
            includeGen.parse();
            typeRegistry.addAll(includeGen.getTypeRegistry());
        }

        registerVisitor(new ServiceVisitor());
        registerVisitor(new TypeVisitor(javaNamespace, typeRegistry));

        for (DocumentVisitor visitor : visitors.values()) {
            document.visit(visitor);
        }
    }

    public void generate() throws IOException
    {
        final TypeToJavaConverter typeConverter = new TypeToJavaConverter(typeRegistry);

        final List<Service> services = ((ServiceVisitor) visitors.get("service")).getServices();
        for (Service service : services) {
            final SwiftJavaType javaType = typeRegistry.findType(thriftNamespace, service.getName());
            final SwiftJavaType parentType = typeRegistry.findType(service.getParent().orNull());

            final StringTemplate interfaceTemplate = templateLoader.load("interface");
            interfaceTemplate.setAttribute("namespace", javaType.getPackage());
            interfaceTemplate.setAttribute("name", javaType.getSimpleName());
            interfaceTemplate.setAttribute("parent", parentType.getClassName());
            System.out.println(interfaceTemplate.toString());

            for (ThriftMethod method : service.getMethods()) {
                final StringBuilder sb = new StringBuilder();
                sb.append(typeConverter.convertType(method.getReturnType())).append(" ");
                sb.append(mangleCase(method.getName())).append("(");
                for (final Iterator<ThriftField> it = method.getArguments().iterator(); it.hasNext(); ) {
                    final ThriftField field = it.next();
                    sb.append(typeConverter.convertType(field.getType())).append(" ").append(mangleCase(field.getName()));
                    if (it.hasNext()) {
                        sb.append(", ");
                    }
                }
                sb.append(")");

                if (!method.getThrowsFields().isEmpty()) {
                    sb.append("\n    throws ");
                    for (final Iterator<ThriftField> it = method.getThrowsFields().iterator(); it.hasNext(); ) {
                        final ThriftField field = it.next();
                        sb.append(typeConverter.convertType(field.getType()));
                        if (it.hasNext()) {
                            sb.append(", ");
                        }
                    }
                }
                sb.append(";\n");
                System.out.println(sb);
            }
        }
    }

    private String mangleCase(final String name)
    {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name.toLowerCase(Locale.ENGLISH));
    }

    private void registerVisitor(final DocumentVisitor visitor)
    {
        visitors.put(visitor.getName(), visitor);
    }

    private TypeRegistry getTypeRegistry()
    {
        return typeRegistry;
    }
}