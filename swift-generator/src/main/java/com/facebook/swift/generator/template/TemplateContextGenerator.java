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
package com.facebook.swift.generator.template;

import com.facebook.swift.generator.ConstantRenderer;
import com.facebook.swift.generator.SwiftDocumentContext;
import com.facebook.swift.generator.SwiftGeneratorConfig;
import com.facebook.swift.generator.SwiftGeneratorTweak;
import com.facebook.swift.generator.SwiftJavaType;
import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.generator.TypeToJavaConverter;
import com.facebook.swift.parser.model.AbstractStruct;
import com.facebook.swift.parser.model.Const;
import com.facebook.swift.parser.model.IntegerEnum;
import com.facebook.swift.parser.model.IntegerEnumField;
import com.facebook.swift.parser.model.Service;
import com.facebook.swift.parser.model.StringEnum;
import com.facebook.swift.parser.model.ThriftField;
import com.facebook.swift.parser.model.ThriftMethod;
import com.google.common.base.Preconditions;

import java.util.HashSet;
import java.util.Set;

import static com.facebook.swift.generator.util.SwiftInternalStringUtils.isBlank;


public class TemplateContextGenerator
{
    private static final MethodContext CLOSE_METHOD_CONTEXT = new MethodContext(null, true, "close", "void", "Void", false /* allow async = false */);

    private final SwiftDocumentContext swiftDocumentContext;
    private final SwiftGeneratorConfig generatorConfig;
    private final TypeRegistry typeRegistry;
    private final TypeToJavaConverter typeConverter;
    private final String defaultNamespace;
    private final ConstantRenderer constantRenderer;

    public TemplateContextGenerator(
            final SwiftDocumentContext swiftDocumentContext,
            final SwiftGeneratorConfig generatorConfig,
            final TypeRegistry typeRegistry,
            final TypeToJavaConverter typeConverter,
            final ConstantRenderer constantRenderer,
            final String defaultNamespace)
    {
        this.swiftDocumentContext = swiftDocumentContext;
        this.generatorConfig = generatorConfig;
        this.typeRegistry = typeRegistry;
        this.defaultNamespace = defaultNamespace;
        this.constantRenderer = constantRenderer;
        this.typeConverter = typeConverter;
    }

    public ServiceContext serviceFromThrift(final Service service)
    {
        final String name = mangleJavaTypeName(service.getName());
        final SwiftJavaType javaType = typeRegistry.findType(defaultNamespace, service.getName());
        final SwiftJavaType parentType = typeRegistry.findType(defaultNamespace, service.getParent().orNull());

        final Set<String> javaParents = new HashSet<>();
        if (parentType != null) {
            javaParents.add(parentType.getClassName());
        }
        final boolean addCloseableInterface = generatorConfig.containsTweak(SwiftGeneratorTweak.ADD_CLOSEABLE_INTERFACE);
        if (addCloseableInterface) {
            javaParents.add("Closeable");
        }
        final ServiceContext serviceContext = new ServiceContext(swiftDocumentContext,
                                                                 name,
                                                                 javaType.getPackage(),
                                                                 javaType.getSimpleName(),
                                                                 javaParents);

        if (addCloseableInterface) {
            serviceContext.addMethod(CLOSE_METHOD_CONTEXT);
        }

        return serviceContext;
    }

    public StructContext structFromThrift(final AbstractStruct struct)
    {
        final String thriftTypeName = struct.getName();
        final SwiftJavaType javaType = typeRegistry.findType(defaultNamespace, thriftTypeName);

        return new StructContext(swiftDocumentContext,
                                 thriftTypeName,
                                 javaType.getPackage(),
                                 javaType.getSimpleName());
    }

    public MethodContext methodFromThrift(final ThriftMethod method)
    {
        return new MethodContext(method.getName(),
                                 method.isOneway(),
                                 mangleJavaMethodName(method.getName()),
                                 typeConverter.convertType(method.getReturnType()),
                                 typeConverter.convert(
                                     method.getReturnType(),
                                     // Use non primitive type if use async client, so it can be used as
                                     // generic parameter for ListenableFuture
                                     false
                                 )
        );
    }

    public FieldContext fieldFromThrift(final ThriftField field)
    {
        Preconditions.checkState(field.getIdentifier().isPresent(), "exception %s has no identifier!", field.getName());

        boolean isOptional = field.getRequiredness() == ThriftField.Requiredness.OPTIONAL;

        return new FieldContext(field.getName(),
                                field.getAnnotations(),
                                field.getRequiredness(),
                                field.isRecursive(),
                                field.getIdentifier().get().shortValue(),
                                typeConverter.convert(field.getType(), !isOptional),
                                mangleJavaMethodName(field.getName()),
                                getterName(field),
                                setterName(field),
                                testPresenceName(field));
    }

    public ConstantsContext constantsFromThrift()
    {
        final String thriftTypeName = "Constants";
        final SwiftJavaType javaType = typeRegistry.findType(defaultNamespace, thriftTypeName);

        return new ConstantsContext(swiftDocumentContext,
                                    thriftTypeName,
                                    javaType.getPackage(),
                                    javaType.getSimpleName());
    }

    public ConstantContext constantFromThrift(final Const constant)
    {
        return new ConstantContext(constant.getName(),
                                   typeConverter.convertType(constant.getType()),
                                   constant.getName(),
                                   constantRenderer.render(constant));
    }

    public ExceptionContext exceptionFromThrift(final ThriftField field)
    {
        Preconditions.checkState(field.getIdentifier().isPresent(), "exception %s has no identifier!", field.getName());
        return new ExceptionContext(typeConverter.convertType(field.getType()), field.getIdentifier().get().shortValue());
    }

    public EnumContext enumFromThrift(final IntegerEnum integerEnum)
    {
        final String thriftTypeName = integerEnum.getName();
        final SwiftJavaType javaType = typeRegistry.findType(defaultNamespace, thriftTypeName);
        return new EnumContext(swiftDocumentContext, javaType.getPackage(), javaType.getSimpleName());
    }

    public EnumContext enumFromThrift(final StringEnum stringEnum)
    {
        final String thriftTypeName = stringEnum.getName();
        final SwiftJavaType javaType = typeRegistry.findType(defaultNamespace, thriftTypeName);
        return new EnumContext(swiftDocumentContext, javaType.getPackage(), javaType.getSimpleName());
    }

    public EnumFieldContext fieldFromThrift(final IntegerEnumField field)
    {
        return new EnumFieldContext(mangleJavaConstantName(field.getName()), field.getValue());
    }

    public EnumFieldContext fieldFromThrift(final String value)
    {
        return new EnumFieldContext(mangleJavaConstantName(value), null);
    }

    /**
     * Turn an incoming snake case name into camel case for use in a java method name.
     *
     * The general idea is: first letter lowercase (unless name starts with multiple caps
     * letters), drop underscores and capitalize the following letter, otherwise don't touch case.
     */
    public static final String mangleJavaMethodName(final String src)
    {
        return mangleJavaName(src, false);
    }

    /**
     * Turn an incoming snake case name into camel case for use in a java type name
     *
     * The general idea is: first letter of type name capitalized, drop underscores and capitalize
     * the following letter, otherwise don't touch case.
     */
    public static final String mangleJavaTypeName(final String src)
    {
        return mangleJavaName(src, true);
    }

    private static final String mangleJavaName(final String src, boolean capitalize)
    {
        Preconditions.checkArgument(!isBlank(src), "input name must not be blank!");

        final StringBuilder sb = new StringBuilder();
        // When we are capitalizing, initialize upCase to true in order to force first
        // character to upper case
        boolean upCase = capitalize;
        // On the other hand, force first character to lowercase, unless both first AND
        // second characters are uppercase (this is so we don't mess up any poorly-named
        // method that might start with acronyms)
        boolean downCase = !capitalize && !nameStartsWithAcronym(src);
        for (int i = 0; i < src.length(); i++) {
            if (src.charAt(i) == '_') {
                upCase = true;
                continue;
            }
            else {
                char ch = src.charAt(i);
                ch = downCase ? Character.toLowerCase(src.charAt(i)) : ch;
                ch = upCase ? Character.toUpperCase(src.charAt(i)) : ch;
                sb.append(ch);
                upCase = false;
                downCase = false;
            }
        }
        return sb.toString();
    }

    public static final String mangleJavaConstantName(final String src)
    {
        final StringBuilder sb = new StringBuilder();
        if (!isBlank(src)) {
            boolean lowerCase = false;
            for (int i = 0; i < src.length(); i++) {
                if (Character.isUpperCase(src.charAt(i))) {
                    if (lowerCase) {
                        sb.append('_');
                    }
                    sb.append(Character.toUpperCase(src.charAt(i)));
                    lowerCase = false;
                }
                else if (Character.isLowerCase(src.charAt(i))) {
                    sb.append(Character.toUpperCase(src.charAt(i)));
                    lowerCase = true;
                }
                else {
                    // Not a letter (e.g. underscore) just emit it
                    sb.append(src.charAt(i));
                }
            }
        }
        return sb.toString();
    }

    private static boolean nameStartsWithAcronym(String name)
    {
        if (name.length() <= 1) {
            return false;
        }
        if (Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            return true;
        }
        return false;
    }

    private String getterName(final ThriftField field)
    {
        final String type = typeConverter.convertType(field.getType());
        return ("boolean".equals(type) ? "is" : "get") + mangleJavaTypeName(field.getName());
    }

    private String setterName(final ThriftField field)
    {
        return "set" + mangleJavaTypeName(field.getName());
    }

    private String testPresenceName(final ThriftField field)
    {
        return "isSet" + mangleJavaTypeName(field.getName());
    }

}
