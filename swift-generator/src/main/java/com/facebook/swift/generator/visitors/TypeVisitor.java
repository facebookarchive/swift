/*
 * Copyright 2012 Facebook, Inc.
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
import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.generator.template.ContextGenerator;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.facebook.swift.parser.visitor.Nameable;
import com.facebook.swift.parser.visitor.Visitable;

public class TypeVisitor implements DocumentVisitor
{
    private final TypeRegistry typeRegistry;
    private final String javaNamespace;
    private final String defaultThriftNamespace;

    public TypeVisitor(final String javaNamespace,
                       final SwiftDocumentContext context)
    {
        this.javaNamespace = javaNamespace;
        this.defaultThriftNamespace = context.getNamespace();
        this.typeRegistry = context.getTypeRegistry();
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
        typeRegistry.add(new SwiftJavaType(defaultThriftNamespace, ContextGenerator.mangleTypeName(type.getName()), javaNamespace));
    }
}
