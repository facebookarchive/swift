package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

public class Document
{
    private final Header header;
    private final List<Definition> definitions;

    public Document(Header header, List<Definition> definitions)
    {
        this.header = checkNotNull(header, "header");
        this.definitions = ImmutableList.copyOf(checkNotNull(definitions, "definitions"));
    }

    public Header getHeader()
    {
        return header;
    }

    public List<Definition> getDefinitions()
    {
        return definitions;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("header", header)
                .add("definitions", definitions)
                .toString();
    }

    public static Document emptyDocument()
    {
        List<String> includes = emptyList();
        List<Namespace> namespaces = emptyList();
        List<String> cppIncludes = emptyList();
        Header header = new Header(includes, namespaces, cppIncludes);
        List<Definition> definitions = emptyList();
        return new Document(header, definitions);
    }
}
