package com.facebook.swift.generator.visitors;

import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.generator.template.ContextGenerator;
import com.facebook.swift.generator.template.EnumContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.IntegerEnum;
import com.facebook.swift.parser.model.IntegerEnumField;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.facebook.swift.parser.visitor.Visitable;
import com.google.common.base.Charsets;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class IntegerEnumVisitor implements DocumentVisitor
{
    private final TemplateLoader templateLoader;
    private final ContextGenerator contextGenerator;

    private final File outputFolder;

    public IntegerEnumVisitor(final TemplateLoader templateLoader, final TypeRegistry typeRegistry, final File outputFolder)
    {
        this.templateLoader = templateLoader;
        this.contextGenerator = new ContextGenerator(typeRegistry);
        this.outputFolder = outputFolder;
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

        final StringTemplate enumTemplate = templateLoader.load("intEnum");
        enumTemplate.setAttribute("enum", enumContext);

        final File serviceFile = new File(outputFolder, integerEnum.getName() + ".java");

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(serviceFile), Charsets.UTF_8)) {
            enumTemplate.write(new NoIndentWriter(osw));
            osw.flush();
        }
    }
}
