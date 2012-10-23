package com.facebook.swift.generator.visitors;

import java.io.File;
import java.io.IOException;

import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.generator.template.MethodContext;
import com.facebook.swift.generator.template.ServiceContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.Service;
import com.facebook.swift.parser.model.ThriftField;
import com.facebook.swift.parser.model.ThriftMethod;
import com.facebook.swift.parser.visitor.Visitable;

public class ServiceVisitor extends AbstractTemplateVisitor
{
    public ServiceVisitor(final TemplateLoader templateLoader, final TypeRegistry typeRegistry, final File outputFolder)
    {
        super(templateLoader, typeRegistry, outputFolder);
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

        render(serviceContext, "service");
    }
}
