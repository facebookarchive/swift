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
package com.facebook.swift.parser.model;

import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    public void visit(final DocumentVisitor visitor) throws IOException
    {
        Preconditions.checkNotNull(visitor, "the visitor must not be null!");

        for (Definition definition : definitions) {
            if (visitor.accept(definition)) {
                definition.visit(visitor);
            }
        }
    }


    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("header", header)
                          .add("definitions", definitions)
                          .toString();
    }

    public static Document emptyDocument()
    {
        List<String> includes = emptyList();
        List<String> cppIncludes = emptyList();
        String defaultNamespace = null;
        Map<String, String> namespaces = Collections.emptyMap();
        Header header = new Header(includes, cppIncludes, defaultNamespace, namespaces);
        List<Definition> definitions = emptyList();
        return new Document(header, definitions);
    }
}
