package com.facebook.swift.generator.visitors;

import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.generator.template.ContextGenerator;
import com.facebook.swift.generator.template.EnumContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.StringEnum;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.facebook.swift.parser.visitor.Visitable;
import com.google.common.base.Charsets;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class StringEnumVisitor implements DocumentVisitor
{
    private final TemplateLoader templateLoader;
    private final ContextGenerator contextGenerator;

    private final File outputFolder;

    public StringEnumVisitor(final TemplateLoader templateLoader, final TypeRegistry typeRegistry, final File outputFolder)
    {
        this.templateLoader = templateLoader;
        this.contextGenerator = new ContextGenerator(typeRegistry);
        this.outputFolder = outputFolder;
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

        final StringTemplate enumTemplate = templateLoader.load("stringEnum");
        enumTemplate.setAttribute("enum", enumContext);

        final File serviceFile = new File(outputFolder, enumContext.getJavaName() + ".java");

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(serviceFile), Charsets.UTF_8)) {
            enumTemplate.write(new NoIndentWriter(osw));
            osw.flush();
        }
    }
}
