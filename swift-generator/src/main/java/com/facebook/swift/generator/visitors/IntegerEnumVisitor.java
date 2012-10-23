package com.facebook.swift.generator.visitors;

import java.io.File;
import java.io.IOException;

import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.generator.template.EnumContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.IntegerEnum;
import com.facebook.swift.parser.model.IntegerEnumField;
import com.facebook.swift.parser.visitor.Visitable;

public class IntegerEnumVisitor extends AbstractTemplateVisitor
{
    public IntegerEnumVisitor(final TemplateLoader templateLoader, final TypeRegistry typeRegistry, final File outputFolder)
    {
        super(templateLoader, typeRegistry, outputFolder);
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable.getClass() == IntegerEnum.class;
    }

    @Override
    public void visit(final Visitable visitable)
        throws IOException
    {
        final IntegerEnum integerEnum = IntegerEnum.class.cast(visitable);
        final EnumContext enumContext = contextGenerator.enumFromThrift(integerEnum);

        for (final IntegerEnumField field : integerEnum.getFields()) {
            enumContext.addField(contextGenerator.fieldFromThrift(field));
        }

        render(enumContext, "intEnum");
    }
}
