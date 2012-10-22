package com.facebook.swift.generator.visitors;

import com.facebook.swift.generator.SwiftJavaType;
import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.facebook.swift.parser.visitor.Nameable;
import com.facebook.swift.parser.visitor.Visitable;

public class TypeVisitor implements DocumentVisitor
{
    private final TypeRegistry typeRegistry;
    private final String javaNamespace;

    public TypeVisitor(final String javaNamespace, final TypeRegistry typeRegistry)
    {
        this.javaNamespace = javaNamespace;
        this.typeRegistry = typeRegistry;
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
        typeRegistry.add(new SwiftJavaType(typeRegistry.getDefaultThriftNamespace(), type.getName(), javaNamespace));
    }
}
