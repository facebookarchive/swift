package com.facebook.swift.generator.util;

import com.google.common.base.Charsets;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class TemplateLoader
{
    private static final Logger LOG = LoggerFactory.getLogger(TemplateLoader.class);

    private final StringTemplateErrorListener ERROR_LISTENER = new LoaderErrorListener();

    private final String templateFileName;

    private volatile StringTemplateGroup stg = null;

    public TemplateLoader(final String templateFileName)
    {
        this.templateFileName = templateFileName;
    }

    public StringTemplate load(final String templateName) throws IOException
    {
        final StringTemplateGroup stg = getTemplateGroup();
        return stg.getInstanceOf(templateName);
    }

    protected StringTemplateGroup getTemplateGroup() throws IOException
    {
        if (stg == null) {
            final URL resourceUrl = Resources.getResource(this.getClass(), "/templates/" + templateFileName);
            final InputSupplier<InputStreamReader> is = Resources.newReaderSupplier(resourceUrl, Charsets.UTF_8);
            stg = new StringTemplateGroup(is.getInput(), AngleBracketTemplateLexer.class, ERROR_LISTENER);
        }

        return stg;
    }

    private static class LoaderErrorListener implements StringTemplateErrorListener
    {
        @Override
        public void error(String arg0, Throwable arg1)
        {
            LOG.error(String.format("%s: %s", arg0, arg1));
        }

        @Override
        public void warning(String arg0)
        {
            LOG.warn(arg0);
        }
    }
}