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
    private final TypedefRegistry typedefRegistry;

    public SwiftDocumentContext(final File thriftFile,
                 final String namespace,
                 final TypeRegistry typeRegistry,
                 final TypedefRegistry typedefRegistry) throws IOException
    {
        this.document = ThriftIdlParser.parseThriftIdl(Files.newReaderSupplier(thriftFile, Charsets.UTF_8));
        this.namespace = namespace;
        this.typeRegistry = typeRegistry;
        this.typedefRegistry = typedefRegistry;
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

    public TypedefRegistry getTypedefRegistry()
    {
        return typedefRegistry;
    }
}

