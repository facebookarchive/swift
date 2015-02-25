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
package com.facebook.swift.parser;

import com.facebook.swift.parser.antlr.DocumentGenerator;
import com.facebook.swift.parser.antlr.ThriftLexer;
import com.facebook.swift.parser.antlr.ThriftParser;
import com.facebook.swift.parser.model.Document;
import com.google.common.io.CharSource;
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
    public static Document parseThriftIdl(CharSource input)
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

    static Tree parseTree(CharSource input)
            throws IOException
    {
        try (Reader reader = input.openStream()) {
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
