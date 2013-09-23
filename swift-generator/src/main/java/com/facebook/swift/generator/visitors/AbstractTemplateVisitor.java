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
package com.facebook.swift.generator.visitors;

import com.facebook.swift.generator.SwiftDocumentContext;
import com.facebook.swift.generator.SwiftGeneratorConfig;
import com.facebook.swift.generator.SwiftGeneratorTweak;
import com.facebook.swift.generator.template.JavaContext;
import com.facebook.swift.generator.template.TemplateContextGenerator;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;

import org.stringtemplate.v4.AutoIndentWriter;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

public abstract class AbstractTemplateVisitor implements DocumentVisitor
{
    private final File outputFolder;
    private final TemplateLoader templateLoader;
    protected final TemplateContextGenerator contextGenerator;
    protected final SwiftGeneratorConfig config;

    protected AbstractTemplateVisitor(final TemplateLoader templateLoader,
                                      final SwiftDocumentContext context,
                                      final SwiftGeneratorConfig config,
                                      final File outputFolder)
    {
        this.outputFolder = outputFolder;
        this.templateLoader = templateLoader;
        this.contextGenerator = context.getTemplateContextGenerator();
        this.config = config;
    }

    protected void render(final JavaContext context, final String templateName)
        throws IOException
    {
        final ST template = templateLoader.load(templateName);
        checkState(template != null, "No template for '%s' found!", templateName);
        template.add("context", context);

        final Map<String, Boolean> tweakMap = new HashMap<>();
        for (SwiftGeneratorTweak tweak: SwiftGeneratorTweak.values()) {
            tweakMap.put(tweak.name(), config.containsTweak(tweak));
        }
        template.add("tweaks", tweakMap);


        final Iterable<String> packages = Splitter.on('.').split(context.getJavaPackage());
        File folder = outputFolder;

        for (String pkg : packages) {
            folder = new File(folder, pkg);
            folder.mkdir();
        }

        final File file = new File(folder, context.getJavaName() + ".java");

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
            template.write(new AutoIndentWriter(osw));
            osw.flush();
        }
    }

    @Override
    public void finish() throws IOException
    {
    }
}

