package com.facebook.swift.parser.visitor;

import java.io.IOException;
import java.util.Collection;

public interface Visitable
{
    void visit(DocumentVisitor visitor) throws IOException;

    public static final class Utils
    {
        private Utils()
        {
        }

        public static void visitAll(final DocumentVisitor visitor, final Collection<? extends Visitable> visitables) throws IOException
        {
            if (visitables != null && !visitables.isEmpty()) {
                for (Visitable visitable : visitables) {
                    if (visitor.accept(visitable)) {
                        visitable.visit(visitor);
                    }
                }
            }
        }

        public static void visit(final DocumentVisitor visitor, final Visitable visitable) throws IOException
        {
            visitor.visit(visitable);
        }
    }
}
