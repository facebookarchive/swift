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
import com.facebook.swift.generator.SwiftJavaType;
import com.facebook.swift.generator.TypeToJavaConverter;
import com.facebook.swift.generator.template.TemplateContextGenerator;
import com.facebook.swift.parser.model.Typedef;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.facebook.swift.parser.visitor.Nameable;
import com.facebook.swift.parser.visitor.Visitable;
import com.google.common.base.Preconditions;
import io.airlift.log.Logger;

public class TypeVisitor implements DocumentVisitor
{
    private static final Logger LOG = Logger.get(TypeVisitor.class);
    private final String javaNamespace;
    private final SwiftDocumentContext documentContext;

    public TypeVisitor(final String javaNamespace,
                       final SwiftDocumentContext documentContext)
    {
        this.javaNamespace = javaNamespace;
        this.documentContext = documentContext;
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable instanceof Nameable;
    }

    @Override
    public void visit(final Visitable visitable)
    {
        final Nameable type = Nameable.class.cast(visitable);
        final SwiftJavaType swiftJavaType = new SwiftJavaType(
                documentContext.getNamespace(),
                TemplateContextGenerator.mangleJavaTypeName(type.getName()),
                type.getName(),
                javaNamespace);
        if (visitable instanceof Typedef) {
            // Typedef checks must be done before the type is added to the registry. Otherwise it would be possible
            // to have a typedef point at itself.
            final Typedef typedef = Typedef.class.cast(visitable);

            LOG.debug("Checking typedef '%s' as '%s'.", typedef.getType(), typedef.getName());

            final TypeToJavaConverter typeConverter = documentContext.getTypeConverter();

            Preconditions.checkNotNull(typeConverter.convertType(typedef.getType()), "typedef %s uses unknown type %s!", typedef.getName(), typedef.getType().toString());
            documentContext.getTypedefRegistry().add(swiftJavaType, typedef.getType());
        }

        LOG.debug("Registering type '%s'", swiftJavaType);
        documentContext.getTypeRegistry().add(swiftJavaType);
    }

    @Override
    public void finish()
    {
    }
}
