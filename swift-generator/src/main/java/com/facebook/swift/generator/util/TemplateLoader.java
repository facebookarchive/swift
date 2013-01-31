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
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
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
import java.io.StringReader;

import javax.annotation.Nonnull;

public class TemplateLoader
{
    private static final Function<String,InputSupplier<InputStreamReader>> FILE_TO_INPUT_SUPPLIER_TRANSFORM =
            new Function<String, InputSupplier<InputStreamReader>>()
            {
                @Nonnull
                @Override
                public InputSupplier<InputStreamReader> apply(@Nonnull String templateFileName)
                {

                    return Resources.newReaderSupplier(Resources.getResource(this.getClass(), "/templates/" + templateFileName), Charsets.UTF_8);
                }
            };

    private static final Logger LOG = LoggerFactory.getLogger(TemplateLoader.class);

    private final StringTemplateErrorListener ERROR_LISTENER = new LoaderErrorListener();

    private final Iterable<String> templateFileNames;
    private volatile StringTemplateGroup stg = null;

    public TemplateLoader(final Iterable<String> templateFileNames)
    {
        this.templateFileNames = templateFileNames;
    }

    public StringTemplate load(final String templateName) throws IOException
    {
        final StringTemplateGroup stg = getTemplateGroup(templateFileNames);
        return stg.getInstanceOf(templateName);
    }

    protected StringTemplateGroup getTemplateGroup(Iterable<String> templateFileNames) throws IOException
    {
        if (stg == null) {
            // Create a hard-coded input supplier for the 'group' header line
            InputSupplier<StringReader> headerSupplier = CharStreams.newReaderSupplier("group swiftGeneratorTemplates;");

            // Convert set of relative paths to .st files into a set of input suppliers
            Iterable<InputSupplier<InputStreamReader>> templateInputSuppliers =
                    Iterables.transform(templateFileNames, FILE_TO_INPUT_SUPPLIER_TRANSFORM);

            // Combine the header and all .st files and load everything into a StringTemplateGroup
            stg = new StringTemplateGroup(
                    CharStreams.join(headerSupplier, CharStreams.join(templateInputSuppliers)).getInput(),
                    AngleBracketTemplateLexer.class,
                    ERROR_LISTENER);
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
