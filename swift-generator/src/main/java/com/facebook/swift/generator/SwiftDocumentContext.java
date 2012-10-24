package com.facebook.swift.generator;

import com.facebook.swift.parser.ThriftIdlParser;
import com.facebook.swift.parser.model.Document;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

public class SwiftDocumentContext
{
    private final Document document;
    private final String namespace;
    private final TypeRegistry typeRegistry;

    public SwiftDocumentContext(final File thriftFile,
                 final String namespace,
                 final TypeRegistry typeRegistry) throws IOException
    {
        this.document = ThriftIdlParser.parseThriftIdl(Files.newReaderSupplier(thriftFile, Charsets.UTF_8));
        this.namespace = namespace;
        this.typeRegistry = typeRegistry;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public Document getDocument()
    {
        return document;
    }

    public TypeRegistry getTypeRegistry()
    {
        return typeRegistry;
    }
}

