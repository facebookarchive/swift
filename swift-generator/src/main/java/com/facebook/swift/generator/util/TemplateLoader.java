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

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.misc.ErrorManager;
import org.stringtemplate.v4.misc.STMessage;

import java.io.IOException;
import java.net.URL;

public class TemplateLoader
{
    private static final Logger LOG = LoggerFactory.getLogger(TemplateLoader.class);

    private static final String COMMON_TEMPLATES = "java/common.st";

    private final STErrorListener ERROR_LISTENER = new LoaderErrorListener();

    private final String templateFileName;

    private volatile STGroup stg = null;

    public TemplateLoader(final String templateFileName)
    {
        this.templateFileName = templateFileName;
    }

    public ST load(final String templateName) throws IOException
    {
        final STGroup stg = getTemplateGroup();
        return stg.getInstanceOf(templateName);
    }

    protected STGroup getTemplateGroup() throws IOException
    {
        if (stg == null) {
            STGroup common = getTemplateGroupFromFile(COMMON_TEMPLATES);
            stg = getTemplateGroupFromFile(templateFileName);
            stg.importTemplates(common);
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

    private static class LoaderErrorListener implements STErrorListener
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
