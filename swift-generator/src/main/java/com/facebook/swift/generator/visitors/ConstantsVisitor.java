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
import com.facebook.swift.generator.template.ConstantsContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.Const;
import com.facebook.swift.parser.visitor.Visitable;
import io.airlift.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ConstantsVisitor extends AbstractTemplateVisitor
{
    private static final Logger LOG = Logger.get(ConstantsVisitor.class);
    private final Set<Const> constants = new HashSet<>();

    public ConstantsVisitor(final TemplateLoader templateLoader,
                            final SwiftDocumentContext context,
                            final SwiftGeneratorConfig config,
                            final File outputFolder)
    {
        super(templateLoader, context, config, outputFolder);
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable.getClass() == Const.class;
    }

    @Override
    public void visit(final Visitable visitable) throws IOException
    {
        final Const constant = Const.class.cast(visitable);
        constants.add(constant);
    }

    @Override
    public void finish()
        throws IOException
    {
        if (constants.size() > 0) {

            final ConstantsContext constantContext = contextGenerator.constantsFromThrift();

            for (final Const constant : constants) {
                try {
                    constantContext.addConstant(contextGenerator.constantFromThrift(constant));
                }
                catch (IllegalStateException e) {
                    LOG.error("Could not generate code for constant '%s' due to https://github.com/facebook/swift/issues/209, skipping...", constant.getName());
                }
            }
            render(constantContext, "constants");
        }
    }
}
