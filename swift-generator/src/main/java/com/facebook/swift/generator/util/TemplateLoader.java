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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import io.airlift.log.Logger;
import org.stringtemplate.v4.AttributeRenderer;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.misc.ErrorManager;
import org.stringtemplate.v4.misc.STMessage;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class TemplateLoader
{
    private static final Logger LOG = Logger.get(TemplateLoader.class);

    private final STErrorListener ERROR_LISTENER = new ErrorListener();

    private final Iterable<String> templateFileNames;
    private volatile STGroup stg = null;
    private Map<Class<?>, ? extends AttributeRenderer> attributeRenderers = ImmutableMap.of();

    public TemplateLoader(final Iterable<String> templateFileNames)
    {
        this.templateFileNames = templateFileNames;
    }

    public TemplateLoader(final Iterable<String> templateFileNames,
                          final Map<Class<?>, ? extends AttributeRenderer> attributeRenderers)
    {
        this(templateFileNames);
        this.attributeRenderers = attributeRenderers;
    }

    public ST load(final String templateName) throws IOException
    {
        final STGroup stg = getTemplateGroup(templateFileNames);
        return stg.getInstanceOf(templateName);
    }

    protected STGroup getTemplateGroup(Iterable<String> templateFileNames) throws IOException
    {
        if (stg == null) {
            // Combine the header and all .st files and load everything into a StringTemplateGroup
            stg = new STGroup();
            stg.setListener(ERROR_LISTENER);
            for (String templateFileName : templateFileNames) {
                stg.importTemplates(getTemplateGroupFromFile(templateFileName));
            }
            for (Map.Entry<Class<?>, ? extends AttributeRenderer> entry : attributeRenderers.entrySet()) {
                Class<?> cls = entry.getKey();
                AttributeRenderer renderer = entry.getValue();
                stg.registerRenderer(cls, renderer);
            }
        }

        return stg;
    }

    protected STGroup getTemplateGroupFromFile(String fileName) throws IOException
    {
        final URL resourceUrl = Resources.getResource(this.getClass(), "/templates/" + fileName);
        STGroup stg = new STGroup();
        stg.errMgr = new ErrorManager(ERROR_LISTENER);
        stg.loadGroupFile("", resourceUrl.toExternalForm());
        return stg;
    }

    private static class ErrorListener implements STErrorListener
    {
        @Override
        public void compileTimeError(STMessage msg)
        {
            LOG.error(msg.toString());
        }

        @Override
        public void runTimeError(STMessage msg)
        {
            LOG.error(msg.toString());
        }

        @Override
        public void IOError(STMessage msg)
        {
            LOG.error(msg.toString());
        }

        @Override
        public void internalError(STMessage msg)
        {
            LOG.error(msg.toString());
        }
    }
}
