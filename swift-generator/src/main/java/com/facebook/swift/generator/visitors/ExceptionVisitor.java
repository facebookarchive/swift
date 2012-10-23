package com.facebook.swift.generator.visitors;

import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.generator.template.ContextGenerator;
import com.facebook.swift.generator.template.StructContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.ThriftException;
import com.facebook.swift.parser.model.ThriftField;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.facebook.swift.parser.visitor.Visitable;
import com.google.common.base.Charsets;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class ExceptionVisitor implements DocumentVisitor
{
    private final TemplateLoader templateLoader;
    private final ContextGenerator contextGenerator;

    private final File outputFolder;

    public ExceptionVisitor(final TemplateLoader templateLoader, final TypeRegistry typeRegistry, final File outputFolder)
    {
        this.templateLoader = templateLoader;
        this.contextGenerator = new ContextGenerator(typeRegistry);
        this.outputFolder = outputFolder;
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable.getClass() == ThriftException.class;
    }

    @Override
    public void visit(final Visitable visitable)
        throws IOException
    {
        final ThriftException exception = ThriftException.class.cast(visitable);
        final StructContext exceptionContext = contextGenerator.structFromThrift(exception);

        for (final ThriftField field : exception.getFields()) {
            exceptionContext.addField(contextGenerator.fieldFromThrift(field));
        }

        final StringTemplate exceptionTemplate = templateLoader.load("exception");
        exceptionTemplate.setAttribute("exception", exceptionContext);

        final File serviceFile = new File(outputFolder, exceptionContext.getJavaName() + ".java");

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(serviceFile), Charsets.UTF_8)) {
            exceptionTemplate.write(new NoIndentWriter(osw));
            osw.flush();
        }
    }
}
