package com.facebook.swift.parser;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import org.testng.annotations.Test;

import java.io.InputStreamReader;

import static com.facebook.swift.parser.ThriftIdlParser.parseThriftIdl;
import static com.facebook.swift.parser.ThriftIdlParser.parseTree;
import static com.facebook.swift.parser.TreePrinter.treeToString;

public class TestDocument
{
    @Test
    public void testEmpty()
            throws Exception
    {
        parseThriftIdl(CharStreams.newReaderSupplier(""));
    }

    @Test
    public void testDocumentFb303()
            throws Exception
    {
        // TODO: verify document
        System.out.println(treeToString(parseTree(resourceReader("fb303.thrift"))));
        System.out.println(parseThriftIdl(resourceReader("fb303.thrift")));
    }

    @Test
    public void testDocumentHbase()
            throws Exception
    {
        // TODO: verify document
        System.out.println(treeToString(parseTree(resourceReader("Hbase.thrift"))));
        System.out.println(parseThriftIdl(resourceReader("Hbase.thrift")));
    }

    private static InputSupplier<InputStreamReader> resourceReader(String name)
    {
        return Resources.newReaderSupplier(Resources.getResource(name), Charsets.UTF_8);
    }
}
