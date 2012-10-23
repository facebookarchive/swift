package com.facebook.swift.generator.visitors;

import java.io.File;
import java.io.IOException;

import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.generator.template.EnumContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.StringEnum;
import com.facebook.swift.parser.visitor.Visitable;

public class StringEnumVisitor extends AbstractTemplateVisitor
{
    public StringEnumVisitor(final TemplateLoader templateLoader, final TypeRegistry typeRegistry, final File outputFolder)
    {
        super(templateLoader, typeRegistry, outputFolder);
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable.getClass() == StringEnum.class;
    }

    @Override
    public void visit(final Visitable visitable)
        throws IOException
    {
        final StringEnum stringEnum = StringEnum.class.cast(visitable);
        final EnumContext enumContext = contextGenerator.enumFromThrift(stringEnum);

        for (final String value : stringEnum.getValues()) {
            enumContext.addField(contextGenerator.fieldFromThrift(value));
        }

        render(enumContext, "stringEnum");
    }
}
