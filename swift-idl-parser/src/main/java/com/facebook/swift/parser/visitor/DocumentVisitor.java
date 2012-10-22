package com.facebook.swift.parser.visitor;

import java.io.IOException;


/**
 * Document visitor to collect information from the Document tree.
 */
public interface DocumentVisitor
{
    boolean accept(Visitable visitable);

    void visit(Visitable visitable) throws IOException;
}
