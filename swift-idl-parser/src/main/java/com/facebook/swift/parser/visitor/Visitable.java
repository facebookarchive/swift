package com.facebook.swift.parser.visitor;

import java.util.Collection;

public interface Visitable
{
    void visit(DocumentVisitor visitor);

    public static final class Utils
    {
        private Utils()
        {
        }

        public static void visitAll(final DocumentVisitor visitor, final Collection<? extends Visitable> visitables)
        {
            if (visitables != null && !visitables.isEmpty()) {
                for (Visitable visitable : visitables) {
                    if (visitor.accept(visitable)) {
                        visitable.visit(visitor);
                    }
                }
            }
        }

        public static void visit(final DocumentVisitor visitor, final Visitable visitable)
        {
            visitor.visit(visitable);
        }
    }
}