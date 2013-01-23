/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
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

    private static final String COMMON_TEMPLATES = "common.st";

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
            StringTemplateGroup common = getTemplateGroupFromFile(COMMON_TEMPLATES);
            stg = getTemplateGroupFromFile(templateFileName);
            stg.setSuperGroup(common);
        }

        return stg;
    }

    protected StringTemplateGroup getTemplateGroupFromFile(String fileName) throws IOException
    {
        final URL resourceUrl = Resources.getResource(this.getClass(), "/templates/" + fileName);
        final InputSupplier<InputStreamReader> is = Resources.newReaderSupplier(resourceUrl, Charsets.UTF_8);
        return new StringTemplateGroup(is.getInput(), AngleBracketTemplateLexer.class, ERROR_LISTENER);
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
