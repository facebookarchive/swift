package com.facebook.swift.generator.visitors;

import com.facebook.swift.generator.SwiftDocumentContext;
import com.facebook.swift.generator.template.ContextGenerator;
import com.facebook.swift.generator.template.JavaContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.google.common.base.Charsets;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplate;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public abstract class AbstractTemplateVisitor implements DocumentVisitor
{
    private final File outputFolder;
    private final TemplateLoader templateLoader;
    protected final ContextGenerator contextGenerator;

    protected AbstractTemplateVisitor(final TemplateLoader templateLoader,
                                      final SwiftDocumentContext context,
                                      final File outputFolder)
    {
        this.outputFolder = outputFolder;
        this.templateLoader = templateLoader;
        this.contextGenerator = new ContextGenerator(context);
    }

    protected void render(final JavaContext context, final String templateName)
        throws IOException
    {
        final StringTemplate template = templateLoader.load(templateName);
        template.setAttribute("context", context);

        final String [] packages = StringUtils.split(context.getJavaPackage(), '.');
        File folder = outputFolder;
        for (String pkg : packages) {
            folder = new File(folder, pkg);
            folder.mkdir();
        }

        final File file = new File(folder, context.getJavaName() + ".java");

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
            template.write(new NoIndentWriter(osw));
            osw.flush();
        }
    }
}

