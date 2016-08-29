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
import com.facebook.swift.codec.ThriftUnion;
import com.facebook.swift.codec.ThriftUnionId;
import com.facebook.swift.codec.metadata.ThriftStructMetadata.MetadataType;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.concurrent.NotThreadSafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.facebook.swift.codec.ThriftField.Requiredness;
import static com.facebook.swift.codec.metadata.FieldKind.THRIFT_UNION_ID;
import static com.facebook.swift.codec.metadata.ReflectionHelper.findAnnotatedMethods;

@NotThreadSafe
public class ThriftUnionMetadataBuilder
    extends AbstractThriftMetadataBuilder
{
    public ThriftUnionMetadataBuilder(ThriftCatalog catalog, Type structType)
    {
        super(catalog, structType);

        // verify the class is public and has the correct annotations
        verifyClass(ThriftUnion.class);

        // extract the @ThriftUnionId fields
        extractThriftUnionId();

        // finally normalize the field metadata using things like
        normalizeThriftFields(catalog);
    }

    @Override
    protected String extractName()
    {
        ThriftUnion annotation = getStructClass().getAnnotation(ThriftUnion.class);
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
        ThriftUnion annotation = getStructClass().getAnnotation(ThriftUnion.class);
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
        ThriftUnion annotation = getStructClass().getAnnotation(ThriftUnion.class);
        if (annotation != null && !annotation.builder().equals(void.class)) {
            return annotation.builder();
        }
        else {
            return null;
        }
    }

    private void extractThriftUnionId()
    {
        Collection<Field> idFields = ReflectionHelper.findAnnotatedFields(getStructClass(), ThriftUnionId.class);
        Collection<Method> idMethods = findAnnotatedMethods(getStructClass(), ThriftUnionId.class);

        if (idFields.size() + idMethods.size() != 1) {
            if (idFields.size() + idMethods.size() == 0) {
                metadataErrors.addError("Neither a field nor a method is annotated with @ThriftUnionId");
            }
            else  if (idFields.size() > 1) {
                metadataErrors.addError("More than one @ThriftUnionId field present");
            }
            else if (idMethods.size() > 1) {
                metadataErrors.addError("More than one @ThriftUnionId method present");
            }
            else {
                metadataErrors.addError("Both fields and methods annotated with @ThriftUnionId");
            }
            return;
        }

        for (Field idField : idFields) {
            FieldExtractor fieldExtractor = new FieldExtractor(structType, idField, null, THRIFT_UNION_ID);
            fields.add(fieldExtractor);
            extractors.add(fieldExtractor);

            FieldInjection fieldInjection = new FieldInjection(structType, idField, null, THRIFT_UNION_ID);
            fields.add(fieldInjection);
            fieldInjections.add(fieldInjection);
        }

        for (Method idMethod: idMethods) {
            if (!Modifier.isPublic(idMethod.getModifiers())) {
                metadataErrors.addError("@ThriftUnionId method [%s] is not public", idMethod.toGenericString());
                continue;
            }
            if (Modifier.isStatic(idMethod.getModifiers())) {
                metadataErrors.addError("@ThriftUnionId method [%s] is static", idMethod.toGenericString());
                continue;
            }

            if (isValidateGetter(idMethod)) {
                MethodExtractor methodExtractor = new MethodExtractor(structType, idMethod, null, THRIFT_UNION_ID);
                fields.add(methodExtractor);
                extractors.add(methodExtractor);
            }
        }
    }

    @Override
    protected void validateConstructors()
    {
        for (ConstructorInjection constructorInjection : constructorInjections) {
            if (constructorInjection.getParameters().size() > 1) {
                metadataErrors.addError("@ThriftConstructor [%s] takes %d arguments, this is illegal for an union",
                                        constructorInjection.getConstructor().toGenericString(),
                                        constructorInjection.getParameters().size());
            }
        }
    }

    @Override
    protected boolean isValidateSetter(Method method)
    {
        // Unions only allow setters with exactly one parameters
        return method.getParameterTypes().length == 1;
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
        ThriftConstructorInjection constructorInjection = buildConstructorInjection();

        // fields injections
        Iterable<ThriftFieldMetadata> fieldsMetadata = buildFieldInjections();

        // methods injections
        List<ThriftMethodInjection> methodInjections = buildMethodInjections();

        return new ThriftStructMetadata(
                structName,
                extractStructIdlAnnotations(),
                structType,
                builderType,
                MetadataType.UNION,
                Optional.fromNullable(builderMethodInjection),
                ImmutableList.copyOf(documentation),
                ImmutableList.copyOf(fieldsMetadata),
                Optional.fromNullable(constructorInjection),
                methodInjections
        );
    }

    private ThriftConstructorInjection buildConstructorInjection()
    {
        for (ConstructorInjection constructorInjection : constructorInjections) {
            if (constructorInjection.getParameters().size() == 0) {
                return new ThriftConstructorInjection(constructorInjection.getConstructor(), buildParameterInjections(constructorInjection.getParameters()));
            }
        }

        // This is actually legal for a ThriftUnion, all c'tors available take arguments and are associated with the FieldMetadata...
        return null;
    }

    @Override
    protected ThriftFieldMetadata buildField(Collection<FieldMetadata> input)
    {
        short id = -1;
        boolean isLegacyId = false;
        String name = null;
        boolean recursiveness = false;
        Requiredness requiredness = Requiredness.UNSPECIFIED;
        Map<String, String> idlAnnotations = null;
        FieldKind fieldType = FieldKind.THRIFT_FIELD;
        ThriftTypeReference thriftTypeReference = null;
        ThriftConstructorInjection thriftConstructorInjection = null;
        ThriftMethodInjection thriftMethodInjection = null;

        // process field injections and extractions
        ImmutableList.Builder<ThriftInjection> injections = ImmutableList.builder();
        ThriftExtraction extraction = null;
        for (FieldMetadata fieldMetadata : input) {
            id = fieldMetadata.getId();
            isLegacyId = fieldMetadata.isLegacyId();
            name = fieldMetadata.getName();
            recursiveness = fieldMetadata.isRecursiveReference();
            requiredness = fieldMetadata.getRequiredness();
            idlAnnotations = fieldMetadata.getIdlAnnotations();
            fieldType = fieldMetadata.getType();
            thriftTypeReference = catalog.getFieldThriftTypeReference(fieldMetadata);

            switch (requiredness) {
                case REQUIRED:
                case OPTIONAL:
                    metadataErrors.addError(
                            "Thrift union '%s' field '%s(%s)' should not be marked required or optional",
                            structName,
                            name,
                            id);
                    break;

                default:
                    break;
            }

            if (fieldMetadata instanceof FieldInjection) {
                FieldInjection fieldInjection = (FieldInjection) fieldMetadata;
                injections.add(new ThriftFieldInjection(fieldInjection.getId(), fieldInjection.getName(), fieldInjection.getField(), fieldInjection.getType()));
            }
            else if (fieldMetadata instanceof ParameterInjection) {
                ParameterInjection parameterInjection = (ParameterInjection) fieldMetadata;
                ThriftParameterInjection thriftParameterInjection =  new ThriftParameterInjection(
                        parameterInjection.getId(),
                        parameterInjection.getName(),
                        parameterInjection.getParameterIndex(),
                        fieldMetadata.getJavaType()
                );
                injections.add(thriftParameterInjection);

                for (ConstructorInjection constructorInjection : constructorInjections) {
                    if (constructorInjection.getParameters().size() == 1 && constructorInjection.getParameters().get(0).equals(parameterInjection)) {
                        thriftConstructorInjection = new ThriftConstructorInjection(constructorInjection.getConstructor(), thriftParameterInjection);
                        break;
                    }
                }

                for (MethodInjection methodInjection : methodInjections) {
                    if (methodInjection.getParameters().size() == 1 && methodInjection.getParameters().get(0).equals(parameterInjection)) {
                        thriftMethodInjection = new ThriftMethodInjection(methodInjection.getMethod(), thriftParameterInjection);
                    }
                }
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

        ThriftFieldMetadata thriftFieldMetadata = new ThriftFieldMetadata(
                id,
                isLegacyId,
                recursiveness,
                requiredness,
                idlAnnotations,
                thriftTypeReference,
                name,
                fieldType,
                injections.build(),
                Optional.fromNullable(thriftConstructorInjection),
                Optional.fromNullable(thriftMethodInjection),
                Optional.fromNullable(extraction),
                Optional.fromNullable(coercion)
        );
        return thriftFieldMetadata;
    }
}
