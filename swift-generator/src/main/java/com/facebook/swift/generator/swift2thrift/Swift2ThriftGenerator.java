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

import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.ThriftField.Requiredness;
import com.facebook.swift.codec.ThriftProtocolType;
import com.facebook.swift.codec.metadata.FieldKind;
import com.facebook.swift.codec.metadata.ReflectionHelper;
import com.facebook.swift.codec.metadata.ThriftFieldMetadata;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;
import com.facebook.swift.codec.metadata.ThriftType;
import com.facebook.swift.codec.metadata.ThriftTypeReference;
import com.facebook.swift.generator.swift2thrift.template.FieldRequirednessRenderer;
import com.facebook.swift.generator.swift2thrift.template.ThriftContext;
import com.facebook.swift.generator.swift2thrift.template.ThriftServiceMetadataRenderer;
import com.facebook.swift.generator.swift2thrift.template.ThriftTypeRenderer;
import com.facebook.swift.generator.util.TemplateLoader;
import com.facebook.swift.service.ThriftService;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import io.airlift.log.Logger;
import org.stringtemplate.v4.AutoIndentWriter;
import org.stringtemplate.v4.ST;

import javax.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class Swift2ThriftGenerator
{
    private static final Logger LOG = Logger.get(Swift2ThriftGenerator.class);
    private final OutputStreamWriter outputStreamWriter;
    private final boolean verbose;
    private final ThriftCodecManager codecManager = new ThriftCodecManager();
    private final String defaultPackage;
    private final String allowMultiplePackages;     // null means don't allow
    private ThriftTypeRenderer thriftTypeRenderer;
    private ArrayList<ThriftType> thriftTypes = Lists.newArrayList();
    private ArrayList<ThriftServiceMetadata> thriftServices = Lists.newArrayList();
    private String packageName;
    // includeMap maps a ThriftType or a ThriftServiceMetadata to the include that defines it
    private Map<Object, String> includeMap = Maps.newHashMap();
    private Set<ThriftType> usedIncludedTypes = Sets.newHashSet();
    private Set<ThriftServiceMetadata> usedIncludedServices = Sets.newHashSet();
    private Set<ThriftType> knownTypes = Sets.newHashSet(builtInKnownTypes);
    private Set<ThriftServiceMetadata> knownServices = Sets.newHashSet();
    private Map<String, String> namespaceMap;
    private boolean recursive;
    private static final Set<ThriftType> builtInKnownTypes =
        ImmutableSet.of(ThriftType.BOOL, ThriftType.BYTE, ThriftType.I16, ThriftType.I32,
            ThriftType.I64, ThriftType.DOUBLE, ThriftType.STRING, ThriftType.BINARY,
            new ThriftType(ThriftType.BOOL, Boolean.class), new ThriftType(ThriftType.BYTE, Byte.class),
            new ThriftType(ThriftType.I16, Short.class), new ThriftType(ThriftType.I32, Integer.class),
            new ThriftType(ThriftType.I64, Long.class), new ThriftType(ThriftType.DOUBLE, Double.class),
            new ThriftType(ThriftType.STRING, String.class), new ThriftType(ThriftType.BINARY, byte[].class));

    Swift2ThriftGenerator(final Swift2ThriftGeneratorConfig config) throws FileNotFoundException
    {
        this.verbose = config.isVerbose();
        String defaultPackage = config.getDefaultPackage();

        if (defaultPackage.isEmpty()) {
            this.defaultPackage = "";
        } else {
            this.defaultPackage = defaultPackage + ".";
        }

        OutputStream os = config.getOutputFile() != null ? new FileOutputStream(config.getOutputFile()) : System.out;
        this.outputStreamWriter = new OutputStreamWriter(os, Charsets.UTF_8);
        Map<String, String> paramIncludeMap = config.getIncludeMap();
        // create a type renderer with an empty map until we build it
        this.thriftTypeRenderer = new ThriftTypeRenderer(ImmutableMap.<ThriftType,String>of());
        for (Map.Entry<String, String> entry: paramIncludeMap.entrySet()) {
            Class<?> cls = load(entry.getKey());
            if (cls == null) {
                continue;
            }

            Object result = convertToThrift(cls);
            if (result != null) {
                this.includeMap.put(result, entry.getValue());
            }
        }

        this.namespaceMap = config.getNamespaceMap();
        this.allowMultiplePackages = config.isAllowMultiplePackages();
        this.recursive = config.isRecursive();
    }

    @SuppressWarnings("PMD.CollapsibleIfStatements")
    public void parse(Iterable<String> inputs) throws IOException
    {
        boolean loadErrors = false;

        if (allowMultiplePackages != null) {
            packageName = allowMultiplePackages;
        }

        for (String className: inputs) {
            Class<?> cls = load(className);
            if (cls == null) {
                loadErrors = true;
                continue;
            }

            if (packageName == null) {
                packageName = cls.getPackage().getName();
            } else if (!packageName.equals(cls.getPackage().getName())) {
                if (allowMultiplePackages == null) {
                    throw new IllegalStateException(
                        String.format("Class %s is in package %s, previous classes were in package %s",
                            cls.getName(), cls.getPackage().getName(), packageName));
                }
            }

            Object result = convertToThrift(cls);
            if (result instanceof ThriftType) {
                thriftTypes.add((ThriftType)result);
            } else if (result instanceof ThriftServiceMetadata) {
                thriftServices.add((ThriftServiceMetadata)result);
            }
            // if the class we just loaded was also in the include map, remove it from there
            includeMap.remove(result);
        }
        if (loadErrors) {
            LOG.error("Couldn't load some classes");
            return;
        }

        if (verify()) {
            gen();
        } else {
            LOG.error("Errors found during verification.");
        }
    }

    private String getFullClassName(String className)
    {
        if (className.indexOf('.') == -1) {
            return defaultPackage + className;
        } else {
            return className;
        }
    }

    private boolean verify()
    {
        if (recursive) {
            // Call verifyStruct and verifyService until the lists of discovered types and services stop changing.
            // This populates the list with all transitive dependencies of the input types/services to be fed into the
            // topological sort of verifyTypes() and verifyServices().
            int len;
            do {
                len = thriftTypes.size();
                for (int i = 0; i < len; i++) {
                    verifyStruct(thriftTypes.get(i), true);
                }
            } while (len != thriftTypes.size());
            do {
                len = thriftServices.size();
                for (int i = 0; i < len; i++) {
                    verifyService(thriftServices.get(i), true);
                }
            } while (len != thriftServices.size());
            recursive = false;
            usedIncludedTypes.clear();
            usedIncludedServices.clear();
            knownTypes = Sets.newHashSet(builtInKnownTypes);
            knownServices.clear();
        }
        // no short-circuit
        return verifyTypes() & verifyServices();
    }

    // verifies that all types are known (in thriftTypes or in include map)
    // and does a topological sort of thriftTypes in dependency order
    private boolean verifyTypes()
    {
        SuccessAndResult<ThriftType> output = topologicalSort(thriftTypes, new Predicate<ThriftType>()
        {
            @Override
            public boolean apply(@Nullable ThriftType t)
            {
                ThriftProtocolType proto = checkNotNull(t).getProtocolType();
                if (proto == ThriftProtocolType.ENUM || proto == ThriftProtocolType.STRUCT) {
                    return verifyStruct(t, true);
                } else {
                    Preconditions.checkState(false, "Top-level non-enum and non-struct?");
                    return false;   // silence compiler
                }
            }
        });
        if (output.success) {
            thriftTypes = output.result;
            return true;
        } else {
            for (ThriftType t: output.result) {
                // we know it's gonna fail, we just want the precise error message
                verifyStruct(t, false);
            }
            return false;
        }
    }

    private boolean verifyServices()
    {
        SuccessAndResult<ThriftServiceMetadata> output = topologicalSort(thriftServices, new Predicate<ThriftServiceMetadata>()
        {
            @Override
            public boolean apply(@Nullable ThriftServiceMetadata thriftServiceMetadata)
            {
                return verifyService(thriftServiceMetadata, true);
            }
        });
        if (output.success) {
            thriftServices = output.result;
            return true;
        } else {
            for (ThriftServiceMetadata s: output.result) {
                // we know it's gonna fail, we just want the precise error message
                verifyService(s, false);
            }
            return false;
        }
    }

    private class SuccessAndResult<T>
    {
        public boolean success;
        public ArrayList<T> result;
        SuccessAndResult(boolean success, ArrayList<T> result)
        {
            this.success = success;
            this.result = result;
        }
    }

    private <T> SuccessAndResult<T> topologicalSort(ArrayList<T> list, Predicate<T> isKnown)
    {
        ArrayList<T> remaining = list;
        ArrayList<T> newList = Lists.newArrayList();
        int prevSize = 0;
        while (prevSize != remaining.size()) {
            prevSize = remaining.size();
            ArrayList<T> bad = Lists.newArrayList();
            for (T t: remaining) {
                if (isKnown.apply(t)) {
                    newList.add(t);
                }
                else {
                    bad.add(t);
                }
            }
            remaining = bad;
        }
        if (prevSize == 0) {
            return new SuccessAndResult<>(true, newList);
        } else {
            return new SuccessAndResult<>(false, remaining);
        }
    }

    private boolean verifyService(ThriftServiceMetadata service, boolean quiet)
    {
        boolean ok = true;
        List<ThriftServiceMetadata> parents = service.getParentServices();

        Preconditions.checkState(
                parents.size() <= 1,
                "service " + service.getName() + " extends multiple services (thrift IDL does not support multiple inheritance for services)", service.getName());

        ThriftServiceMetadata parent = parents.size() == 0 ? null : parents.get(0);

        if (parent != null && !knownServices.contains(parent)) {
            if (includeMap.containsKey(parent)) {
                usedIncludedServices.add(parent);
            } else {
                ok = false;
                if (!quiet) {
                    LOG.error("Unknown parent service %s in %s",
                            parent.getName(),
                            service.getName());
                }
            }
        }

        for (Map.Entry<String, ThriftMethodMetadata> method : service.getDeclaredMethods().entrySet()) {
            for (ThriftFieldMetadata f : method.getValue().getParameters()) {
                if (!verifyField(f.getThriftType())) {
                    ok = false;
                    if (!quiet) {
                        LOG.error("Unknown argument type %s in %s.%s",
                                thriftTypeRenderer.toString(f.getThriftType()),
                                service.getName(),
                                method.getKey());
                    }
                }
            }

            for (ThriftType ex : method.getValue().getExceptions().values()) {
                if (!verifyField(ex)) {
                    ok = false;
                    if (!quiet) {
                        LOG.error("Unknown exception type %s in %s.%s",
                                thriftTypeRenderer.toString(ex),
                                service.getName(),
                                method.getKey());
                    }
                }
            }

            if (!method.getValue().getReturnType().equals(ThriftType.VOID) &&
                    !verifyField(method.getValue().getReturnType())) {
                ok = false;
                if (!quiet) {
                    LOG.error("Unknown return type %s in %s.%s",
                            thriftTypeRenderer.toString(method.getValue().getReturnType()),
                            service.getName(),
                            method.getKey());
                }
            }
        }

        knownServices.add(service);
        return ok;
    }

    private boolean verifyElementType(ThriftTypeReference t)
    {
        if (!recursive && t.isRecursive()) {
            return true;
        }
        else {
            return verifyField(t.get());
        }
    }

    private boolean verifyField(ThriftType t)
    {
        ThriftProtocolType proto = t.getProtocolType();
        if (proto == ThriftProtocolType.SET || proto == ThriftProtocolType.LIST) {
            return verifyElementType(t.getValueTypeReference());
        } else if (proto == ThriftProtocolType.MAP) {
            // no short-circuit
            return verifyElementType(t.getKeyTypeReference()) & verifyElementType(t.getValueTypeReference());
        } else {
            if (knownTypes.contains(t)) {
                return true;
            }

            if (includeMap.containsKey(t)) {
                usedIncludedTypes.add(t);
                return true;
            }

            if (recursive) {
                // recursive but type is unknown - add it to the list and recurse
                thriftTypes.add(t);
                return verifyStruct(t, true);
            }
            return false;
        }
    }

    private boolean verifyStruct(ThriftType t, boolean quiet)
    {
        if (t.getProtocolType() == ThriftProtocolType.ENUM) {
            knownTypes.add(t);
            return true;
        }
        ThriftStructMetadata metadata = t.getStructMetadata();
        boolean ok = true;

        knownTypes.add(t);

        for (ThriftFieldMetadata fieldMetadata: metadata.getFields(FieldKind.THRIFT_FIELD)) {
            if (!recursive && fieldMetadata.isTypeReferenceRecursive()) {
                continue;
            }

            boolean fieldOk = verifyField(fieldMetadata.getThriftType());
            if (!fieldOk) {
                ok = false;
                if (!quiet) {
                    LOG.error("Unknown type %s in %s.%s",
                              thriftTypeRenderer.toString(fieldMetadata.getThriftType()),
                              metadata.getStructName(),
                              fieldMetadata.getName());
                }
            }
        }

        if (!ok) {
            knownTypes.remove(t);
        }
        return ok;
    }

    private Class<?> load(String className)
    {
        className = getFullClassName(className);
        try {
            return getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            LOG.warn("Couldn't load class %s (%s)", className, e);
        }
        return null;
    }

    // returns ThriftType, ThriftServiceMetadata or null
    private Object convertToThrift(Class<?> cls)
    {
        Set<ThriftService> serviceAnnotations = ReflectionHelper.getEffectiveClassAnnotations(cls, ThriftService.class);
        if (!serviceAnnotations.isEmpty()) {
            // it's a service
            ThriftServiceMetadata serviceMetadata = new ThriftServiceMetadata(cls, codecManager.getCatalog());
            if (verbose) {
                LOG.info("Found thrift service: %s", cls.getSimpleName());
            }
            return serviceMetadata;
        } else {
            // it's a type (will throw if it's not)
            ThriftType thriftType = codecManager.getCatalog().getThriftType(cls);
            if (verbose) {
                LOG.info("Found thrift type: %s", thriftTypeRenderer.toString(thriftType));
            }
            return thriftType;
        }
    }

    private void gen() throws IOException
    {
        ImmutableMap.Builder<ThriftType, String> typenameMap = ImmutableMap.builder();
        ImmutableMap.Builder<ThriftServiceMetadata, String> serviceMap = ImmutableMap.builder();
        ImmutableSet.Builder<String> includes = ImmutableSet.builder();
        for (ThriftType t: usedIncludedTypes) {
            String filename = includeMap.get(t);
            includes.add(filename);
            typenameMap.put(t, Files.getNameWithoutExtension(filename));
        }

        for (ThriftServiceMetadata s: usedIncludedServices) {
            String filename = includeMap.get(s);
            includes.add(filename);
            serviceMap.put(s, Files.getNameWithoutExtension(filename));
        }

        this.thriftTypeRenderer = new ThriftTypeRenderer(typenameMap.build());
        ThriftServiceMetadataRenderer serviceRenderer = new ThriftServiceMetadataRenderer(serviceMap.build());
        TemplateLoader tl = new TemplateLoader(ImmutableList.of("thrift/common.st"),
                ImmutableMap.of(
                        ThriftType.class, thriftTypeRenderer,
                        ThriftServiceMetadata.class, serviceRenderer,
                        Requiredness.class, new FieldRequirednessRenderer()));
        ThriftContext ctx = new ThriftContext(packageName, ImmutableList.copyOf(includes.build()), thriftTypes, thriftServices, namespaceMap);
        ST template = tl.load("thriftfile");
        template.add("context", ctx);
        template.write(new AutoIndentWriter(outputStreamWriter));
        outputStreamWriter.flush();
    }

    private ClassLoader getClassLoader()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return ClassLoader.getSystemClassLoader();
    }
}
