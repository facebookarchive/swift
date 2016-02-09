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

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.TypeToken;
import com.google.inject.internal.MoreTypes;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.facebook.swift.codec.ThriftField.Requiredness;
import static com.facebook.swift.codec.metadata.FieldKind.THRIFT_FIELD;
import static com.facebook.swift.codec.metadata.FieldMetadata.extractThriftFieldName;
import static com.facebook.swift.codec.metadata.FieldMetadata.getOrExtractThriftFieldName;
import static com.facebook.swift.codec.metadata.FieldMetadata.getThriftFieldId;
import static com.facebook.swift.codec.metadata.FieldMetadata.getThriftFieldIsLegacyId;
import static com.facebook.swift.codec.metadata.FieldMetadata.getThriftFieldName;
import static com.facebook.swift.codec.metadata.FieldMetadata.getThriftFieldRequiredness;
import static com.facebook.swift.codec.metadata.ReflectionHelper.extractParameterNames;
import static com.facebook.swift.codec.metadata.ReflectionHelper.findAnnotatedMethods;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getAllDeclaredFields;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getAllDeclaredMethods;
import static com.facebook.swift.codec.metadata.ReflectionHelper.resolveFieldTypes;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Sets.newTreeSet;
import static java.util.Arrays.asList;
import static jp.skypencil.guava.stream.GuavaCollectors.toImmutableSet;

@NotThreadSafe
public abstract class AbstractThriftMetadataBuilder
{
    protected final String structName;
    protected final Type structType;
    protected final Type builderType;

    protected final List<String> documentation;
    protected final List<FieldMetadata> fields = newArrayList();

    // readers
    protected final List<Extractor> extractors = newArrayList();

    // writers
    protected final List<MethodInjection> builderMethodInjections = newArrayList();
    protected final List<ConstructorInjection> constructorInjections = newArrayList();
    protected final List<FieldInjection> fieldInjections = newArrayList();
    protected final List<MethodInjection> methodInjections = newArrayList();

    protected final ThriftCatalog catalog;
    protected final MetadataErrors metadataErrors;

    protected AbstractThriftMetadataBuilder(ThriftCatalog catalog, Type structType)
    {
        this.catalog = checkNotNull(catalog, "catalog is null");
        this.structType = checkNotNull(structType, "structType is null");
        this.metadataErrors = new MetadataErrors(catalog.getMonitor());

        // assign the struct name from the annotation or from the Java class
        structName = extractName();
        // get the builder type from the annotation or from the Java class
        builderType = extractBuilderType();
        // grab any documentation from the annotation or saved JavaDocs
        documentation = ThriftCatalog.getThriftDocumentation(getStructClass());
        // extract all of the annotated constructor and report an error if
        // there is more than one or none
        // also extract thrift fields from the annotated parameters and verify
        extractFromConstructors();
        // extract thrift fields from the annotated fields and verify
        extractFromFields();
        // extract thrift fields from the annotated methods (and parameters) and verify
        extractFromMethods();
    }

    protected abstract String extractName();

    protected abstract Map<String, String> extractStructIdlAnnotations();

    protected abstract Class<?> extractBuilderClass();

    protected abstract void validateConstructors();

    protected abstract boolean isValidateSetter(Method method);

    protected abstract ThriftFieldMetadata buildField(Collection<FieldMetadata> input);

    public abstract ThriftStructMetadata build();

    public MetadataErrors getMetadataErrors()
    {
        return metadataErrors;
    }

    public Class<?> getStructClass()
    {
        return TypeToken.of(structType).getRawType();
    }

    public Class<?> getBuilderClass()
    {
        return TypeToken.of(builderType).getRawType();
    }

    private Type extractBuilderType()
    {
        Class<?> builderClass = extractBuilderClass();

        if (builderClass == null) {
            return null;
        }

        if (builderClass.getTypeParameters().length == 0) {
            return builderClass;
        }

        if (!(structType instanceof ParameterizedType)) {
            metadataErrors.addError("Builder class '%s' may only be generic if the type it builds ('%s') is also generic", builderClass.getName(), getStructClass().getName());
            return builderClass;
        }

        if (builderClass.getTypeParameters().length != getStructClass().getTypeParameters().length) {
            metadataErrors.addError("Generic builder class '%s' must have the same number of type parameters as the type it builds ('%s')", builderClass.getName(), getStructClass().getName());
            return builderClass;
        }

        ParameterizedType parameterizedStructType = (ParameterizedType) structType;

        return new MoreTypes.ParameterizedTypeImpl(builderClass.getEnclosingClass(), builderClass, parameterizedStructType.getActualTypeArguments());
    }


    protected final void verifyClass(Class<? extends Annotation> annotation)
    {
        String annotationName = annotation.getSimpleName();
        String structClassName = getStructClass().getName();

        // Verify struct class is public and final
        if (!Modifier.isPublic(getStructClass().getModifiers())) {
            metadataErrors.addError("%s class '%s' is not public", annotationName, structClassName);
        }

        if (!getStructClass().isAnnotationPresent(annotation)) {
            metadataErrors.addError("%s class '%s' does not have a @%s annotation", annotationName, structClassName, annotationName);
        }
    }

    protected final void extractFromConstructors()
    {
        if (builderType == null) {
            // struct class must have a valid constructor
            addConstructors(structType);
        }
        else {
            // builder class must have a valid constructor
            addConstructors(builderType);

            // builder class must have a build method annotated with @ThriftConstructor
            addBuilderMethods();

            // verify struct class does not have @ThriftConstructors
            for (Constructor<?> constructor : getStructClass().getConstructors()) {
                if (constructor.isAnnotationPresent(ThriftConstructor.class)) {
                    metadataErrors.addWarning("Thrift class '%s' has a builder class, but constructor '%s' annotated with @ThriftConstructor", getStructClass().getName(), constructor);
                }
            }
        }
    }

    protected final void addConstructors(Type type)
    {
        Class<?> clazz = TypeToken.of(type).getRawType();

        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.isSynthetic()) {
                continue;
            }
            if (!constructor.isAnnotationPresent(ThriftConstructor.class)) {
                continue;
            }

            if (!Modifier.isPublic(constructor.getModifiers())) {
                metadataErrors.addError("@ThriftConstructor '%s' is not public", constructor.toGenericString());
                continue;
            }

            List<ParameterInjection> parameters = getParameterInjections(
                    type,
                    constructor.getParameterAnnotations(),
                    resolveFieldTypes(structType, constructor.getGenericParameterTypes()),
                    extractParameterNames(constructor));
            if (parameters != null) {
                fields.addAll(parameters);
                constructorInjections.add(new ConstructorInjection(constructor, parameters));
            }
        }

        // add the default constructor
        if (constructorInjections.isEmpty()) {
            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    metadataErrors.addError("Default constructor '%s' is not public", constructor.toGenericString());
                }
                constructorInjections.add(new ConstructorInjection(constructor));
            }
            catch (NoSuchMethodException e) {
                metadataErrors.addError("Struct class '%s' does not have a public no-arg constructor", clazz.getName());
            }
        }

        validateConstructors();
    }

    protected final void addBuilderMethods()
    {
        for (Method method : findAnnotatedMethods(getBuilderClass(), ThriftConstructor.class)) {
            List<ParameterInjection> parameters = getParameterInjections(
                    builderType,
                    method.getParameterAnnotations(),
                    resolveFieldTypes(builderType, method.getGenericParameterTypes()),
                    extractParameterNames(method));

            // parameters are null if the method is misconfigured
            if (parameters != null) {
                fields.addAll(parameters);
                builderMethodInjections.add(new MethodInjection(method, parameters));
            }

            if (!getStructClass().isAssignableFrom(method.getReturnType())) {
                metadataErrors.addError(
                        "'%s' says that '%s' is its builder class, but @ThriftConstructor method '%s' in the builder does not build an instance assignable to that type",
                        structType,
                        builderType,
                        method.getName());
            }
        }

        // find invalid methods not skipped by findAnnotatedMethods()
        for (Method method : getAllDeclaredMethods(getBuilderClass())) {
            if (method.isAnnotationPresent(ThriftConstructor.class) || hasThriftFieldAnnotation(method)) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    metadataErrors.addError("@ThriftConstructor method '%s' is not public", method.toGenericString());
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    metadataErrors.addError("@ThriftConstructor method '%s' is static", method.toGenericString());
                }
            }
        }

        if (builderMethodInjections.isEmpty()) {
            metadataErrors.addError("Struct builder class '%s' does not have a public builder method annotated with @ThriftConstructor", getBuilderClass().getName());
        }
        if (builderMethodInjections.size() > 1) {
            metadataErrors.addError("Multiple builder methods are annotated with @ThriftConstructor ", builderMethodInjections);
        }
    }

    protected final void extractFromFields()
    {
        if (builderType == null) {
            // struct fields are readable and writable
            addFields(getStructClass(), true, true);
        }
        else {
            // builder fields are writable
            addFields(getBuilderClass(), false, true);
            // struct fields are readable
            addFields(getStructClass(), true, false);
        }
    }

    protected final void addFields(Class<?> clazz, boolean allowReaders, boolean allowWriters)
    {
        for (Field fieldField : ReflectionHelper.findAnnotatedFields(clazz, ThriftField.class)) {
            addField(fieldField, allowReaders, allowWriters);
        }

        // find invalid fields not skipped by findAnnotatedFields()
        for (Field field : getAllDeclaredFields(clazz)) {
            if (field.isAnnotationPresent(ThriftField.class)) {
                if (!Modifier.isPublic(field.getModifiers())) {
                    metadataErrors.addError("@ThriftField field '%s' is not public", field.toGenericString());
                }
                if (Modifier.isStatic(field.getModifiers())) {
                    metadataErrors.addError("@ThriftField field '%s' is static", field.toGenericString());
                }
            }
        }
    }

    protected final void addField(Field fieldField, boolean allowReaders, boolean allowWriters)
    {
        checkArgument(fieldField.isAnnotationPresent(ThriftField.class));

        ThriftField annotation = fieldField.getAnnotation(ThriftField.class);
        if (allowReaders) {
            FieldExtractor fieldExtractor = new FieldExtractor(structType, fieldField, annotation, THRIFT_FIELD);
            fields.add(fieldExtractor);
            extractors.add(fieldExtractor);
        }
        if (allowWriters) {
            FieldInjection fieldInjection = new FieldInjection(structType, fieldField, annotation, THRIFT_FIELD);
            fields.add(fieldInjection);
            fieldInjections.add(fieldInjection);
        }
    }

    protected final void extractFromMethods()
    {
        if (builderType != null) {
            // builder methods are writable
            addMethods(builderType, false, true);
            // struct methods are readable
            addMethods(structType, true, false);
        }
        else {
            // struct methods are readable and writable
            addMethods(structType, true, true);
        }
    }

    protected final void addMethods(Type type, boolean allowReaders, boolean allowWriters)
    {
        Class<?> clazz = TypeToken.of(type).getRawType();

        for (Method fieldMethod : findAnnotatedMethods(clazz, ThriftField.class)) {
            addMethod(type, fieldMethod, allowReaders, allowWriters);
        }

        // find invalid methods not skipped by findAnnotatedMethods()
        for (Method method : getAllDeclaredMethods(clazz)) {
            if (method.isAnnotationPresent(ThriftField.class) || hasThriftFieldAnnotation(method)) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    metadataErrors.addError("@ThriftField method '%s' is not public", method.toGenericString());
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    metadataErrors.addError("@ThriftField method '%s' is static", method.toGenericString());
                }
            }
        }
    }

    protected final void addMethod(Type type, Method method, boolean allowReaders, boolean allowWriters)
    {
        checkArgument(method.isAnnotationPresent(ThriftField.class));

        ThriftField annotation = method.getAnnotation(ThriftField.class);
        Class<?> clazz = TypeToken.of(type).getRawType();

        // verify parameters
        if (isValidateGetter(method)) {
            if (allowReaders) {
                MethodExtractor methodExtractor = new MethodExtractor(type, method, annotation, THRIFT_FIELD);
                fields.add(methodExtractor);
                extractors.add(methodExtractor);
            }
            else {
                metadataErrors.addError("Reader method %s.%s is not allowed on a builder class", clazz.getName(), method.getName());
            }
        }
        else if (isValidateSetter(method)) {
            if (allowWriters) {
                List<ParameterInjection> parameters;
                if (method.getParameterTypes().length > 1 || Iterables.any(asList(method.getParameterAnnotations()[0]), Predicates.instanceOf(ThriftField.class))) {
                    parameters = getParameterInjections(
                            type,
                            method.getParameterAnnotations(),
                            resolveFieldTypes(type, method.getGenericParameterTypes()),
                            extractParameterNames(method));
                    if (annotation.value() != Short.MIN_VALUE) {
                        metadataErrors.addError("A method with annotated parameters can not have a field id specified: %s.%s ", clazz.getName(), method.getName());
                    }
                    if (!annotation.name().isEmpty()) {
                        metadataErrors.addError("A method with annotated parameters can not have a field name specified: %s.%s ", clazz.getName(), method.getName());
                    }
                    if (annotation.requiredness() == Requiredness.REQUIRED) {
                        metadataErrors.addError("A method with annotated parameters can not be marked as required: %s.%s ", clazz.getName(), method.getName());
                    }
                }
                else {
                    Type parameterType = resolveFieldTypes(type, method.getGenericParameterTypes())[0];
                    parameters = ImmutableList.of(new ParameterInjection(type, 0, annotation, ReflectionHelper.extractFieldName(method), parameterType));
                }
                fields.addAll(parameters);
                methodInjections.add(new MethodInjection(method, parameters));
            }
            else {
                metadataErrors.addError("Inject method %s.%s is not allowed on struct class, since struct has a builder", clazz.getName(), method.getName());
            }
        }
        else {
            metadataErrors.addError("Method %s.%s is not a supported getter or setter", clazz.getName(), method.getName());
        }
    }

    protected final boolean hasThriftFieldAnnotation(Method method)
    {
        for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
            for (Annotation parameterAnnotation : parameterAnnotations) {
                if (parameterAnnotation instanceof ThriftField) {
                    return true;
                }
            }
        }
        return false;
    }

    protected final boolean isValidateGetter(Method method)
    {
        return method.getParameterTypes().length == 0 && method.getReturnType() != void.class;
    }

    protected final List<ParameterInjection> getParameterInjections(Type type, Annotation[][] parameterAnnotations, Type[] parameterTypes, String[] parameterNames)
    {
        List<ParameterInjection> parameters = newArrayListWithCapacity(parameterAnnotations.length);
        for (int parameterIndex = 0; parameterIndex < parameterAnnotations.length; parameterIndex++) {
            Annotation[] annotations = parameterAnnotations[parameterIndex];
            Type parameterType = parameterTypes[parameterIndex];

            ThriftField thriftField = null;
            for (Annotation annotation : annotations) {
                if (annotation instanceof ThriftField) {
                    thriftField = (ThriftField) annotation;
                }
            }

            ParameterInjection parameterInjection = new ParameterInjection(
                    type,
                    parameterIndex,
                    thriftField,
                    parameterNames[parameterIndex],
                    parameterType
            );

            parameters.add(parameterInjection);
        }
        return parameters;
    }


    protected final void normalizeThriftFields(ThriftCatalog catalog)
    {
        // assign all fields an id (if possible)
        Set<String> fieldsWithConflictingIds = inferThriftFieldIds();

        // group fields by id
        Multimap<Optional<Short>, FieldMetadata> fieldsById = Multimaps.index(fields, getThriftFieldId());
        for (Entry<Optional<Short>, Collection<FieldMetadata>> entry : fieldsById.asMap().entrySet()) {
            Collection<FieldMetadata> fields = entry.getValue();

            // fields must have an id
            if (!entry.getKey().isPresent()) {
                for (String fieldName : newTreeSet(transform(fields, getOrExtractThriftFieldName()))) {
                    // only report errors for fields that don't have conflicting ids
                    if (!fieldsWithConflictingIds.contains(fieldName)) {
                        metadataErrors.addError("Thrift class '%s' fields %s do not have an id", structName, newTreeSet(transform(fields, getOrExtractThriftFieldName())));
                    }
                }
                continue;
            }

            short fieldId = entry.getKey().get();

            // ensure all fields for this ID have the same name
            String fieldName = extractFieldName(fieldId, fields);
            for (FieldMetadata field : fields) {
                field.setName(fieldName);
            }

            // ensure all fields for this ID have the same requiredness
            Requiredness requiredness = extractFieldRequiredness(fieldId, fieldName, fields);
            for (FieldMetadata field : fields) {
                field.setRequiredness(requiredness);
            }

            // We need to do the isLegacyId check in two places. We've already done this
            // process for fields which had multiple `@ThriftField` annotations when we
            // assigned them all the same ID. It doesn't hurt to do it again. On the other
            // hand, we need to do it now to catch the fields which only had a single
            // @ThriftAnnotation, because inferThriftFieldIds skipped them.
            boolean isLegacyId = extractFieldIsLegacyId(fieldId, fieldName, fields);
            for (FieldMetadata field : fields) {
                field.setIsLegacyId(isLegacyId);
            }

            Map<String, String> idlAnnotations = extractFieldIdlAnnotations(fieldId, fields);
            for (FieldMetadata field : fields) {
                field.setIdlAnnotations(idlAnnotations);
            }

            // ensure all fields for this ID have the same non-null get for isRecursiveReference
            boolean isRecursiveReference = extractFieldIsRecursiveReference(fieldId, fields);
            for (FieldMetadata field : fields) {
                field.setIsRecursiveReference(isRecursiveReference);
            }

            // verify fields have a supported java type and all fields
            // for this ID have the same thrift type
            verifyFieldType(fieldId, fieldName, fields, catalog);
        }
    }

    /**
     * Assigns all fields an id if possible.  Fields are grouped by name and for each group, if there
     * is a single id, all fields in the group are assigned this id.  If the group has multiple ids,
     * an error is reported.
     */
    protected final Set<String> inferThriftFieldIds()
    {
        Set<String> fieldsWithConflictingIds = new HashSet<>();

        // group fields by explicit name or by name extracted from field, method or property
        Multimap<String, FieldMetadata> fieldsByExplicitOrExtractedName = Multimaps.index(fields, getOrExtractThriftFieldName());
        inferThriftFieldIds(fieldsByExplicitOrExtractedName, fieldsWithConflictingIds);

        // group fields by name extracted from field, method or property
        // this allows thrift name to be set explicitly without having to duplicate the name on getters and setters
        // todo should this be the only way this works?
        Multimap<String, FieldMetadata> fieldsByExtractedName = Multimaps.index(fields, extractThriftFieldName());
        inferThriftFieldIds(fieldsByExtractedName, fieldsWithConflictingIds);

        return fieldsWithConflictingIds;
    }

    protected final void inferThriftFieldIds(Multimap<String, FieldMetadata> fieldsByName, Set<String> fieldsWithConflictingIds)
    {
        // for each name group, set the ids on the fields without ids
        for (Entry<String, Collection<FieldMetadata>> entry : fieldsByName.asMap().entrySet()) {
            Collection<FieldMetadata> fields = entry.getValue();
            String fieldName = entry.getKey();

            // skip all entries without a name or singleton groups... we'll deal with these later
            if (fields.size() <= 1) {
                continue;
            }

            // all ids used by this named field
            Set<Short> ids = ImmutableSet.copyOf(Optional.presentInstances(transform(fields, getThriftFieldId())));

            // multiple conflicting ids
            if (ids.size() > 1) {
                if (!fieldsWithConflictingIds.contains(fieldName)) {
                    metadataErrors.addError("Thrift class '%s' field '%s' has multiple ids: %s", structName, fieldName, ids.toString());
                    fieldsWithConflictingIds.add(fieldName);
                }
                continue;
            }

            // single id, so set on all fields in this group (groups with no id are handled later),
            // and validate isLegacyId is consistent and correct.
            if (ids.size() == 1) {
                short id = Iterables.getOnlyElement(ids);

                boolean isLegacyId = extractFieldIsLegacyId(id, fieldName, fields);

                // propagate the id data to all fields in this group
                for (FieldMetadata field : fields) {
                    field.setId(id);
                    field.setIsLegacyId(isLegacyId);
                }
            }
        }
    }

    protected final Map<String, String> extractFieldIdlAnnotations(short fieldId, Collection<FieldMetadata> fields)
    {
        Set<Map<String, String>> idlAnnotationMaps =
            fields.stream()
                  .map(field -> field == null ? null : field.getIdlAnnotations())
                  .filter(annotationMap -> annotationMap != null && !annotationMap.isEmpty())
                  .collect(toImmutableSet());

        if (idlAnnotationMaps.isEmpty()) {
            return ImmutableMap.of();
        }

        if (idlAnnotationMaps.size() > 1) {
            metadataErrors.addError("Thrift class '%s' field '%s' has conflicting IDL annotation maps", structName, fieldId);
        }
        return idlAnnotationMaps.iterator().next();
    }

    protected final boolean extractFieldIsRecursiveReference(short fieldId, Collection<FieldMetadata> fields)
    {
        Set<Boolean> isRecursiveReferences =
            fields.stream()
                  .map(FieldMetadata::isRecursiveReference)
                  .filter(value -> value != null)
                  .collect(toImmutableSet());

        if (isRecursiveReferences.isEmpty()) {
            return false;
        }

        if (isRecursiveReferences.size() > 1) {
            metadataErrors.addError("Thrift class '%s' field '%s' has both isRecursiveReference=TRUE and isRecursiveReference=FALSE", structName, fieldId);
        }
        return isRecursiveReferences.iterator().next();
    }

    protected final boolean extractFieldIsLegacyId(short id, String fieldName, Collection<FieldMetadata> fields)
    {
        Set<Boolean> isLegacyIds = ImmutableSet.copyOf(Optional.presentInstances(transform(fields, getThriftFieldIsLegacyId())));

        if (isLegacyIds.size() > 1) {
            metadataErrors.addError("Thrift class '%s' field '%s' has both isLegacyId=true and isLegacyId=false", structName, fieldName);
        }
        if (id < 0) {
            if (! isLegacyIds.contains(true)) {
                metadataErrors.addError("Thrift class '%s' field '%s' has a negative field id but not isLegacyId=true", structName, fieldName);
            }
        } else {
            if (isLegacyIds.contains(true)) {
                metadataErrors.addError("Thrift class '%s' field '%s' has isLegacyId=true but not a negative field id", structName, fieldName);
            }
        }

        return id < 0;
    }

    protected final String extractFieldName(short id, Collection<FieldMetadata> fields)
    {
        // get the names used by these fields
        Set<String> names = ImmutableSet.copyOf(filter(transform(fields, getThriftFieldName()), notNull()));

        String name;
        if (!names.isEmpty()) {
            if (names.size() > 1) {
                metadataErrors.addWarning("Thrift class %s field %s has multiple names %s", structName, id, names);
            }
            name = names.iterator().next();
        }
        else {
            // pick a name for this field
            name = Iterables.find(transform(fields, extractThriftFieldName()), notNull());
        }
        return name;
    }

    protected final Requiredness extractFieldRequiredness(short fieldId, String fieldName, Collection<FieldMetadata> fields)
    {
        Predicate<Requiredness> specificRequiredness = new Predicate<Requiredness>()
        {
            @Override
            public boolean apply(@Nullable Requiredness input)
            {
                return (input != null) && (input != Requiredness.UNSPECIFIED);
            }
        };

        Set<Requiredness> requirednessValues = ImmutableSet.copyOf(filter(transform(fields, getThriftFieldRequiredness()), specificRequiredness));

        if (requirednessValues.size() > 1) {
            metadataErrors.addError("Thrift class '%s' field '%s(%d)' has multiple requiredness values: %s", structName, fieldName, fieldId, requirednessValues.toString());
        }

        Requiredness resolvedRequiredness;
        if (requirednessValues.isEmpty()) {
            resolvedRequiredness = Requiredness.NONE;
        }
        else {
            resolvedRequiredness = requirednessValues.iterator().next();
        }

        return resolvedRequiredness;
    }

    /**
     * Verifies that the the fields all have a supported Java type and that all fields map to the
     * exact same ThriftType.
     */
    protected final void verifyFieldType(short id, String name, Collection<FieldMetadata> fields, ThriftCatalog catalog)
    {
        boolean isSupportedType = true;
        for (FieldMetadata field : fields) {
            if (!catalog.isSupportedStructFieldType(field.getJavaType())) {
                metadataErrors.addError("Thrift class '%s' field '%s(%s)' type '%s' is not a supported Java type", structName, name, id, TypeToken.of(field.getJavaType()));
                isSupportedType = false;
                // only report the error once
                break;
            }
        }

        // fields must have the same type
        if (isSupportedType) {
            Set<ThriftTypeReference> types = new HashSet<>();
            for (FieldMetadata field : fields) {
                types.add(catalog.getFieldThriftTypeReference(field));
            }
            if (types.size() > 1) {
                metadataErrors.addError("Thrift class '%s' field '%s(%s)' has multiple types: %s", structName, name, id, types);
            }
        }
    }


    protected final ThriftMethodInjection buildBuilderConstructorInjections()
    {
        ThriftMethodInjection builderMethodInjection = null;
        if (builderType != null) {
            MethodInjection builderMethod = builderMethodInjections.get(0);
            builderMethodInjection = new ThriftMethodInjection(builderMethod.getMethod(), buildParameterInjections(builderMethod.getParameters()));
        }
        return builderMethodInjection;
    }

    protected final Iterable<ThriftFieldMetadata> buildFieldInjections()
    {
        Multimap<Optional<Short>, FieldMetadata> fieldsById = Multimaps.index(fields, getThriftFieldId());
        return Iterables.transform(fieldsById.asMap().values(), new Function<Collection<FieldMetadata>, ThriftFieldMetadata>()
        {
            @Override
            public ThriftFieldMetadata apply(Collection<FieldMetadata> input)
            {
                checkArgument(!input.isEmpty(), "input is empty");
                return buildField(input);
            }
        });
    }

    protected final List<ThriftMethodInjection> buildMethodInjections()
    {
        return Lists.transform(methodInjections, new Function<MethodInjection, ThriftMethodInjection>()
        {
            @Override
            public ThriftMethodInjection apply(MethodInjection injection)
            {
                return new ThriftMethodInjection(injection.getMethod(), buildParameterInjections(injection.getParameters()));
            }
        });
    }

    protected final List<ThriftParameterInjection> buildParameterInjections(List<ParameterInjection> parameters)
    {
        return Lists.transform(parameters, new Function<ParameterInjection, ThriftParameterInjection>()
        {
            @Override
            public ThriftParameterInjection apply(ParameterInjection injection)
            {
                return new ThriftParameterInjection(
                        injection.getId(),
                        injection.getName(),
                        injection.getParameterIndex(),
                        injection.getJavaType()
                );
            }
        });
    }
}

