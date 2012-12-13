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
import com.facebook.swift.generator.TypedefRegistry;
import com.facebook.swift.generator.template.ContextGenerator;
import com.facebook.swift.parser.model.Typedef;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.facebook.swift.parser.visitor.Visitable;

public class TypedefVisitor implements DocumentVisitor
{
    private final TypedefRegistry typedefRegistry;
    private final String javaNamespace;
    private final String defaultThriftNamespace;

    public TypedefVisitor(final String javaNamespace,
                       final SwiftDocumentContext context)
    {
        this.javaNamespace = javaNamespace;
        this.defaultThriftNamespace = context.getNamespace();
        this.typedefRegistry = context.getTypedefRegistry();
    }

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable instanceof Typedef;
    }

    @Override
    public void visit(final Visitable visitable)
    {
    final Typedef typedef = Typedef.class.cast(visitable);
    typedefRegistry.add(new SwiftJavaType(defaultThriftNamespace, ContextGenerator.mangleJavatypeName(typedef.getName()), javaNamespace),
                typedef.getType());
    }
}
