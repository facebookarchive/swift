package com.facebook.swift.generator.visitors;

import com.facebook.swift.generator.SwiftDocumentContext;
import com.facebook.swift.generator.template.EnumContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.StringEnum;
import com.facebook.swift.parser.visitor.Visitable;

import java.io.File;
import java.io.IOException;

public class StringEnumVisitor extends AbstractTemplateVisitor
{
    public StringEnumVisitor(final TemplateLoader templateLoader,
                             final SwiftDocumentContext context,
                             final File outputFolder)
    {
        super(templateLoader, context, outputFolder);
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable.getClass() == StringEnum.class;
    }

    @Override
    public void visit(final Visitable visitable) throws IOException
    {
        final StringEnum stringEnum = StringEnum.class.cast(visitable);
        final EnumContext enumContext = contextGenerator.enumFromThrift(stringEnum);

        for (final String value : stringEnum.getValues()) {
            enumContext.addField(contextGenerator.fieldFromThrift(value));
        }

        render(enumContext, "stringEnum");
    }
}
