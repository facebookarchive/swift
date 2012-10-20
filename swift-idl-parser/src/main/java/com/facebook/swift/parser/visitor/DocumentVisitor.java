package com.facebook.swift.parser.visitor;


/**
 * Document visitor to collect information from the Document tree.
 */
public interface DocumentVisitor
{
    String getName();

    boolean accept(Visitable visitable);

    void visit(Visitable visitable);
}
