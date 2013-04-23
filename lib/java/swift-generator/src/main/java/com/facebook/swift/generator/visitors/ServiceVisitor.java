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

import com.facebook.swift.generator.SwiftGeneratorTweak;

import com.facebook.swift.generator.SwiftDocumentContext;
import com.facebook.swift.generator.SwiftGeneratorConfig;
import com.facebook.swift.generator.template.ExceptionContext;
import com.facebook.swift.generator.template.MethodContext;
import com.facebook.swift.generator.template.ServiceContext;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.parser.model.Service;
import com.facebook.swift.parser.model.ThriftField;
import com.facebook.swift.parser.model.ThriftMethod;
import com.facebook.swift.parser.visitor.Visitable;

import java.io.File;
import java.io.IOException;

public class ServiceVisitor extends AbstractTemplateVisitor
{
    private static final ExceptionContext THRIFT_EXCEPTION_CONTEXT = ExceptionContext.forType("org.apache.thrift.TException");

    public ServiceVisitor(final TemplateLoader templateLoader,
                          final SwiftDocumentContext context,
                          final SwiftGeneratorConfig config,
                          final File outputFolder)
    {
        super(templateLoader, context, config, outputFolder);
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable.getClass() == Service.class;
    }

    @Override
    public void visit(final Visitable visitable) throws IOException
    {
        final Service service = Service.class.cast(visitable);
        final ServiceContext serviceContext = contextGenerator.serviceFromThrift(service);

        for (ThriftMethod method: service.getMethods()) {
            final MethodContext methodContext = contextGenerator.methodFromThrift(method);
            serviceContext.addMethod(methodContext);

            for (final ThriftField field : method.getArguments()) {
                methodContext.addParameter(contextGenerator.fieldFromThrift(field));
            }

            for (final ThriftField field : method.getThrowsFields()) {
                methodContext.addException(contextGenerator.exceptionFromThrift(field));
            }

            if (config.containsTweak(SwiftGeneratorTweak.ADD_THRIFT_EXCEPTION)) {
                methodContext.addException(THRIFT_EXCEPTION_CONTEXT);
            }
        }

        render(serviceContext, "service");
    }
}
