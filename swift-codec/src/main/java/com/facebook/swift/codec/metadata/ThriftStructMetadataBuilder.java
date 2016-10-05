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
package com.facebook.swift.codec.metadata;

import com.facebook.swift.codec.ThriftIdlAnnotation;
import com.facebook.swift.codec.ThriftStruct;
import com.facebook.swift.codec.metadata.ThriftStructMetadata.MetadataType;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.annotation.concurrent.NotThreadSafe;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.facebook.swift.codec.ThriftField.Requiredness;
import static com.facebook.swift.codec.metadata.FieldKind.THRIFT_FIELD;

@NotThreadSafe
public class ThriftStructMetadataBuilder
    extends AbstractThriftMetadataBuilder
{
    public ThriftStructMetadataBuilder(ThriftCatalog catalog, Type structType)
    {
        super(catalog, structType);

        // verify the class is public and has the correct annotations
        verifyClass(ThriftStruct.class);

        // finally normalize the field metadata using things like
        normalizeThriftFields(catalog);
    }

    @Override
    protected String extractName()
    {
        ThriftStruct annotation = getStructClass().getAnnotation(ThriftStruct.class);
        if (annotation == null) {
            return getStructClass().getSimpleName();
        }
        else if (!annotation.value().isEmpty()) {
            return annotation.value();
        }
        else {
            return getStructClass().getSimpleName();
        }
    }

    @Override
    protected Map<String, String> extractStructIdlAnnotations()
    {
        ThriftStruct annotation = getStructClass().getAnnotation(ThriftStruct.class);
        if (annotation == null) {
            return ImmutableMap.of();
        }
        else {
            ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
            for (ThriftIdlAnnotation idlAnnotation : annotation.idlAnnotations()) {
                builder.put(idlAnnotation.key(), idlAnnotation.value());
            }
            return builder.build();
        }
    }

    @Override
    protected Class<?> extractBuilderClass()
    {
        ThriftStruct annotation = getStructClass().getAnnotation(ThriftStruct.class);
        if (annotation != null && !annotation.builder().equals(void.class)) {
            return annotation.builder();
        }
        else {
            return null;
        }
    }

    @Override
    protected void validateConstructors()
    {
        if (constructorInjections.size() > 1) {
            metadataErrors.addError("Multiple constructors are annotated with @ThriftConstructor ", constructorInjections);
        }
    }

    @Override
    protected boolean isValidateSetter(Method method)
    {
        return method.getParameterTypes().length >= 1;
    }

    //
    // Build final metadata
    //
    @Override
    public ThriftStructMetadata build()
    {
        // this code assumes that metadata is clean
        metadataErrors.throwIfHasErrors();

        // builder constructor injection
        ThriftMethodInjection builderMethodInjection = buildBuilderConstructorInjections();

        // constructor injection (or factory method for builder)
        ThriftConstructorInjection constructorInjections = buildConstructorInjection();

        // fields injections
        Iterable<ThriftFieldMetadata> fieldsMetadata = buildFieldInjections();

        // methods injections
        List<ThriftMethodInjection> methodInjections = buildMethodInjections();

        return new ThriftStructMetadata(
                structName,
                extractStructIdlAnnotations(),
                structType,
                builderType,
                MetadataType.STRUCT,
                Optional.fromNullable(builderMethodInjection),
                ImmutableList.copyOf(documentation),
                ImmutableList.copyOf(fieldsMetadata),
                Optional.of(constructorInjections),
                methodInjections
        );
    }

    private ThriftConstructorInjection buildConstructorInjection()
    {
        return Iterables.getOnlyElement(Lists.transform(constructorInjections, new Function<ConstructorInjection, ThriftConstructorInjection>()
        {
            @Override
            public ThriftConstructorInjection apply(ConstructorInjection injection)
            {
                return new ThriftConstructorInjection(injection.getConstructor(), buildParameterInjections(injection.getParameters()));
            }
        }));
    }

    @Override
    protected ThriftFieldMetadata buildField(Collection<FieldMetadata> input)
    {
        short id = -1;
        boolean isLegacyId = false;
        Map<String, String> idlAnnotations = null;
        String name = null;
        Requiredness requiredness = Requiredness.UNSPECIFIED;
        boolean recursive = false;
        ThriftTypeReference thriftTypeReference = null;

        // process field injections and extractions
        ImmutableList.Builder<ThriftInjection> injections = ImmutableList.builder();
        ThriftExtraction extraction = null;
        for (FieldMetadata fieldMetadata : input) {
            id = fieldMetadata.getId();
            isLegacyId = fieldMetadata.isLegacyId();
            name = fieldMetadata.getName();
            recursive = fieldMetadata.isRecursiveReference();
            requiredness = fieldMetadata.getRequiredness();
            idlAnnotations = fieldMetadata.getIdlAnnotations();
            thriftTypeReference = catalog.getFieldThriftTypeReference(fieldMetadata);

            if (fieldMetadata instanceof FieldInjection) {
                FieldInjection fieldInjection = (FieldInjection) fieldMetadata;
                injections.add(new ThriftFieldInjection(fieldInjection.getId(), fieldInjection.getName(), fieldInjection.getField(), fieldInjection.getType()));
            }
            else if (fieldMetadata instanceof ParameterInjection) {
                ParameterInjection parameterInjection = (ParameterInjection) fieldMetadata;
                injections.add(new ThriftParameterInjection(
                        parameterInjection.getId(),
                        parameterInjection.getName(),
                        parameterInjection.getParameterIndex(),
                        fieldMetadata.getJavaType()
                ));
            }
            else if (fieldMetadata instanceof FieldExtractor) {
                FieldExtractor fieldExtractor = (FieldExtractor) fieldMetadata;
                extraction = new ThriftFieldExtractor(fieldExtractor.getId(), fieldExtractor.getName(), fieldExtractor.getType(), fieldExtractor.getField(), fieldExtractor.getJavaType());
            }
            else if (fieldMetadata instanceof MethodExtractor) {
                MethodExtractor methodExtractor = (MethodExtractor) fieldMetadata;
                extraction = new ThriftMethodExtractor(methodExtractor.getId(), methodExtractor.getName(), methodExtractor.getType(), methodExtractor.getMethod(), methodExtractor.getJavaType());
            }
        }

        // add type coercion
        TypeCoercion coercion = null;
        if (!thriftTypeReference.isRecursive() && thriftTypeReference.get().isCoerced()) {
            coercion = catalog.getDefaultCoercion(thriftTypeReference.get().getJavaType());
        }

        if (recursive && requiredness != Requiredness.OPTIONAL) {
            metadataErrors.addError("Struct '%s' field '%s' is recursive but not marked optional",
                    structName,
                    name);
        }

        ThriftFieldMetadata thriftFieldMetadata = new ThriftFieldMetadata(
                id,
                isLegacyId,
                recursive,
                requiredness,
                idlAnnotations,
                thriftTypeReference,
                name,
                THRIFT_FIELD,
                injections.build(),
                Optional.<ThriftConstructorInjection>absent(),
                Optional.<ThriftMethodInjection>absent(),
                Optional.fromNullable(extraction),
                Optional.fromNullable(coercion)
        );
        return thriftFieldMetadata;
    }
}
