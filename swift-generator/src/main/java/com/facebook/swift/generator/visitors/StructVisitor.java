package com.facebook.swift.generator.visitors;

import com.facebook.swift.generator.SwiftDocumentContext;
import com.facebook.swift.generator.template.StructContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.Struct;
import com.facebook.swift.parser.model.ThriftField;
import com.facebook.swift.parser.visitor.Visitable;

import java.io.File;
import java.io.IOException;

public class StructVisitor extends AbstractTemplateVisitor
{
    public StructVisitor(final TemplateLoader templateLoader,
                         final SwiftDocumentContext context,
                         final File outputFolder)
    {
        super(templateLoader, context, outputFolder);
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable.getClass() == Struct.class;
    }

    @Override
    public void visit(final Visitable visitable) throws IOException
    {
        final Struct struct = Struct.class.cast(visitable);
        final StructContext structContext = contextGenerator.structFromThrift(struct);

        for (final ThriftField field : struct.getFields()) {
            structContext.addField(contextGenerator.fieldFromThrift(field));
        }

        render(structContext, "struct");
    }
}
