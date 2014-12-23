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
package com.facebook.swift.generator.swift2thrift;

import java.io.File;
import java.util.Map;

import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.metadata.AnnotatedThriftServiceMetadataBuilder;
import com.facebook.swift.service.metadata.ThriftServiceMetadataBuilder;

public class Swift2ThriftGeneratorConfig {
    private final File outputFile;
    private final Map<String, String> includeMap;
    private final boolean verbose;
    private final String defaultPackage;
    private final Map<String, String> namespaceMap;
    private final String allowMultiplePackages;
    private final boolean recursive;
    private final ThriftServiceMetadataBuilder serviceMetadataBuilder;
    private final ThriftCodecManager codecManager;
    
    private Swift2ThriftGeneratorConfig(final File outputFile, final Map<String, String> includeMap,
            boolean verbose, String defaultPackage, final Map<String, String> namespaceMap,
            String allowMultiplePackages, boolean recursive, ThriftServiceMetadataBuilder serviceMetadataBuilder, ThriftCodecManager codecManager){
        this.outputFile = outputFile;
        this.includeMap = includeMap;
        this.verbose = verbose;
        this.defaultPackage = defaultPackage;
        this.namespaceMap = namespaceMap;
        this.allowMultiplePackages = allowMultiplePackages;
        this.recursive = recursive;
        this.codecManager = codecManager;
        this.serviceMetadataBuilder = serviceMetadataBuilder;
    }
    

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Returns the output Thrift IDL URI.
     */
    public File getOutputFile()
    {
        return outputFile;
    }

    public Map<String, String> getIncludeMap()
    {
        return includeMap;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    public String getDefaultPackage()
    {
        return defaultPackage;
    }

    public Map<String, String> getNamespaceMap()
    {
        return namespaceMap;
    }

    public String isAllowMultiplePackages()
    {
        return allowMultiplePackages;
    }

    public ThriftServiceMetadataBuilder getServiceMetadataBuilder()
    {
        return serviceMetadataBuilder;
    }

    public boolean isRecursive()
    {
        return recursive;
    }
    
    public ThriftCodecManager getCodecManager()
    {
        return codecManager;
    }

    public static class Builder
    {
        private File outputFile = null;
        private Map<String, String> includeMap;
        private boolean verbose;
        private String defaultPackage;
        private Map<String, String> namespaceMap;
        private String allowMultiplePackages;
        private boolean recursive;
        private ThriftCodecManager codecManager = new ThriftCodecManager();
        private ThriftServiceMetadataBuilder serviceMetadataBuilder = new AnnotatedThriftServiceMetadataBuilder(codecManager.getCatalog());

        private Builder()
        {
        }

        public Swift2ThriftGeneratorConfig build()
        {
            return new Swift2ThriftGeneratorConfig(outputFile, includeMap, verbose, defaultPackage,
                    namespaceMap, allowMultiplePackages, recursive, serviceMetadataBuilder, codecManager);
        }

        public Builder outputFile(final File outputFile)
        {
            this.outputFile = outputFile;
            return this;
        }

        public Builder includeMap(Map<String, String> includeMap)
        {
            this.includeMap = includeMap;
            return this;
        }

        public Builder verbose(boolean verbose)
        {
            this.verbose = verbose;
            return this;
        }

        public Builder defaultPackage(String defaultPackage)
        {
            this.defaultPackage = defaultPackage;
            return this;
        }

        public Builder namespaceMap(Map<String, String> namespaceMap)
        {
            this.namespaceMap = namespaceMap;
            return this;
        }

        public Builder allowMultiplePackages(String allowMultiplePackages)
        {
            this.allowMultiplePackages = allowMultiplePackages;
            return this;
        }

        public Builder recursive(boolean recursive)
        {
            this.recursive = recursive;
            return this;
        }
        
        public Builder codecManager(ThriftCodecManager codecManager)
        {
            this.codecManager = codecManager;
            return this;
        }
        
        public Builder serviceMetadataBuilder(ThriftServiceMetadataBuilder serviceMetadataBuilder) {
            this.serviceMetadataBuilder = serviceMetadataBuilder;
            return this;
        }
        
    }
}
