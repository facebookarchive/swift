package com.facebook.swift.generator.visitors;

import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.generator.template.ContextGenerator;
import com.facebook.swift.generator.template.MethodContext;
import com.facebook.swift.generator.template.ServiceContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.Service;
import com.facebook.swift.parser.model.ThriftField;
import com.facebook.swift.parser.model.ThriftMethod;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.facebook.swift.parser.visitor.Visitable;
import com.google.common.base.Charsets;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class ServiceVisitor implements DocumentVisitor
{
    private final TemplateLoader templateLoader;
    private final ContextGenerator contextGenerator;
    private final File outputFolder;

    public ServiceVisitor(final TemplateLoader templateLoader, final TypeRegistry typeRegistry, final File outputFolder)
    {
        this.templateLoader = templateLoader;
        this.contextGenerator = new ContextGenerator(typeRegistry);
        this.outputFolder = outputFolder;
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable.getClass() == Service.class;
    }

    @Override
    public void visit(final Visitable visitable)
        throws IOException
    {
        final Service service = Service.class.cast(visitable);
        final ServiceContext serviceContext = contextGenerator.serviceFromThrift(service);

        for (ThriftMethod method: service.getMethods()) {
            final MethodContext methodContext = contextGenerator.methodFromThrift(method);
            serviceContext.addMethod(methodContext);

            for (final ThriftField field : method.getArguments()) {
                methodContext.addParameter(contextGenerator.fieldFromThrift(field));
            }

            for (final ThriftField field : method.getThrowsFields()) {
                methodContext.addException(contextGenerator.exceptionFromThrift(field));
            }
        }

        final StringTemplate serviceTemplate = templateLoader.load("service");
        serviceTemplate.setAttribute("service", serviceContext);

        final File serviceFile = new File(outputFolder, service.getName() + ".java");

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(serviceFile), Charsets.UTF_8)) {
            serviceTemplate.write(new NoIndentWriter(osw));
            osw.flush();
        }
    }
}
