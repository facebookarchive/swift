package com.facebook.swift.parser;

import com.facebook.swift.parser.antlr.DocumentGenerator;
import com.facebook.swift.parser.antlr.ThriftLexer;
import com.facebook.swift.parser.antlr.ThriftParser;
import com.facebook.swift.parser.model.Document;
import com.google.common.io.InputSupplier;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.BufferedTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeNodeStream;

import java.io.IOException;
import java.io.Reader;

public class ThriftIdlParser
{
    public static Document parseThriftIdl(InputSupplier<? extends Reader> input)
            throws IOException
    {
        Tree tree = parseTree(input);
        TreeNodeStream stream = new BufferedTreeNodeStream(tree);
        DocumentGenerator generator = new DocumentGenerator(stream);
        try {
            return generator.document().value;
        }
        catch (RecognitionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static Tree parseTree(InputSupplier<? extends Reader> input)
            throws IOException
    {
        try (Reader reader = input.getInput()) {
            ThriftLexer lexer = new ThriftLexer(new ANTLRReaderStream(reader));
            ThriftParser parser = new ThriftParser(new CommonTokenStream(lexer));
            try {
                Tree tree = (Tree) parser.document().getTree();
                if (parser.getNumberOfSyntaxErrors() > 0) {
                    throw new IllegalArgumentException("syntax error");
                }
                return tree;
            }
            catch (RecognitionException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
