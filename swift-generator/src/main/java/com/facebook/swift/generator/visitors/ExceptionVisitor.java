package com.facebook.swift.generator.visitors;

import com.facebook.swift.generator.SwiftDocumentContext;
import com.facebook.swift.generator.template.StructContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.ThriftException;
import com.facebook.swift.parser.model.ThriftField;
import com.facebook.swift.parser.visitor.Visitable;

import java.io.File;
import java.io.IOException;

public class ExceptionVisitor extends AbstractTemplateVisitor
{
    public ExceptionVisitor(final TemplateLoader templateLoader,
                            final SwiftDocumentContext context,
                            final File outputFolder)
    {
        super(templateLoader, context, outputFolder);
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable.getClass() == ThriftException.class;
    }

    @Override
    public void visit(final Visitable visitable) throws IOException
    {
        final ThriftException exception = ThriftException.class.cast(visitable);
        final StructContext exceptionContext = contextGenerator.structFromThrift(exception);

        for (final ThriftField field : exception.getFields()) {
            exceptionContext.addField(contextGenerator.fieldFromThrift(field));
        }

        render(exceptionContext, "exception");
    }
}
