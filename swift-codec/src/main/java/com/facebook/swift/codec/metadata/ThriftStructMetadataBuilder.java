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
import com.facebook.swift.codec.ThriftStruct;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.TypeToken;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static com.facebook.swift.codec.metadata.ReflectionHelper.extractParameterNames;
import static com.facebook.swift.codec.metadata.ReflectionHelper.findAnnotatedMethods;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getAllDeclaredFields;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getAllDeclaredMethods;
import static com.facebook.swift.codec.metadata.ThriftStructMetadataBuilder.FieldMetadata.extractThriftFieldName;
import static com.facebook.swift.codec.metadata.ThriftStructMetadataBuilder.FieldMetadata.getOrExtractThriftFieldName;
import static com.facebook.swift.codec.metadata.ThriftStructMetadataBuilder.FieldMetadata.getThriftFieldId;
import static com.facebook.swift.codec.metadata.ThriftStructMetadataBuilder.FieldMetadata.getThriftFieldName;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Sets.newTreeSet;
import static java.util.Arrays.asList;

@NotThreadSafe
public class ThriftStructMetadataBuilder<T>
{
    private final String structName;
    private final Class<T> structClass;
    private final Class<?> builderClass;

    private final List<String> documentation;
    private final List<FieldMetadata> fields = newArrayList();

    // readers
    private final List<Extractor> extractors = newArrayList();

    // writers
    private final List<MethodInjection> builderMethodInjections = newArrayList();
    private final List<ConstructorInjection> constructorInjections = newArrayList();
    private final List<FieldInjection> fieldInjections = newArrayList();
    private final List<MethodInjection> methodInjections = newArrayList();

    private final ThriftCatalog catalog;
    private final MetadataErrors metadataErrors;

    public ThriftStructMetadataBuilder(ThriftCatalog catalog, Class<T> structClass)
    {
        this.catalog = checkNotNull(catalog, "catalog is null");
        this.structClass = checkNotNull(structClass, "structClass is null");
        this.metadataErrors = new MetadataErrors(catalog.getMonitor());

        // verify the class is public and has the correct annotations
        verifyStructClass();

        // assign the struct name from the annotation or from the Java class
        structName = extractStructName();
        // get the builder class from the annotation or from the Java class
        builderClass = extractBuilderClass();
        // grab any documentation from the annotation or saved JavaDocs
        documentation = ThriftCatalog.getThriftDocumentation(structClass);
        // extract all of the annotated constructor and report an error if
        // there is more than one or none
        // also extract thrift fields from the annotated parameters and verify
        extractFromConstructors();
        // extract thrift fields from the annotated fields and verify
        extractFromFields();
        // extract thrift fields from the annotated methods (and parameters) and verify
        extractFromMethods();

        // finally normalize the field metadata using things like
        normalizeThriftFields(catalog);
    }

    public MetadataErrors getMetadataErrors()
    {
        return metadataErrors;
    }

    private void verifyStructClass()
    {
        // Verify struct class is public and not abstract
        if (Modifier.isAbstract(structClass.getModifiers())) {
            metadataErrors.addError("Struct class [%s] is abstract", structClass.getName());
        }
        if (!Modifier.isPublic(structClass.getModifiers())) {
            metadataErrors.addError("Struct class [%s] is not public", structClass.getName());
        }

        if (!structClass.isAnnotationPresent(ThriftStruct.class)) {
            metadataErrors.addError("Struct class [%s] does not have a @ThriftStruct annotation", structClass.getName());
        }
    }

    private String extractStructName()
    {
        ThriftStruct annotation = structClass.getAnnotation(ThriftStruct.class);
        if (annotation == null) {
            return structClass.getSimpleName();
        }
        else if (!annotation.value().isEmpty()) {
            return annotation.value();
        }
        else {
            return structClass.getSimpleName();
        }
    }

    private Class<?> extractBuilderClass()
    {
        ThriftStruct annotation = structClass.getAnnotation(ThriftStruct.class);
        if (annotation != null && !annotation.builder().equals(void.class)) {
            return annotation.builder();
        }
        else {
            return null;
        }
    }

    private void extractFromConstructors()
    {
        if (builderClass == null) {
            // struct class must have a valid constructor
            addConstructors(structClass);
        }
        else {
            // builder class must have a valid constructor
            addConstructors(builderClass);

            // builder class must have a build method annotated with @ThriftConstructor
            addBuilderMethods();

            // verify struct class does not have @ThriftConstructors
            for (Constructor<?> constructor : structClass.getConstructors()) {
                if (constructor.isAnnotationPresent(ThriftConstructor.class)) {
                    metadataErrors.addWarning("Struct class [%s] has a builder class, but constructor %s annotated with @ThriftConstructor", structClass.getName(), constructor);
                }
            }
        }
    }

    private void addConstructors(Class<?> clazz)
    {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.isSynthetic()) {
                continue;
            }
            if (!constructor.isAnnotationPresent(ThriftConstructor.class)) {
                continue;
            }

            if (!Modifier.isPublic(constructor.getModifiers())) {
                metadataErrors.addError("@ThriftConstructor [%s] is not public", constructor.toGenericString());
                continue;
            }

            List<ParameterInjection> parameters = getParameterInjections(
                    constructor.getParameterAnnotations(),
                    constructor.getGenericParameterTypes(),
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
                    metadataErrors.addError("Default constructor [%s] is not public", constructor.toGenericString());
                }
                constructorInjections.add(new ConstructorInjection(constructor));
            }
            catch (NoSuchMethodException e) {
                metadataErrors.addError("Struct class [%s] does not have a public no-arg constructor", clazz.getName());
            }
        }

        if (constructorInjections.size() > 1) {
            metadataErrors.addError("Multiple constructors are annotated with @ThriftConstructor ", constructorInjections);
        }
    }

    private void addBuilderMethods()
    {
        for (Method method : findAnnotatedMethods(builderClass, ThriftConstructor.class)) {
            List<ParameterInjection> parameters = getParameterInjections(
                    method.getParameterAnnotations(),
                    method.getGenericParameterTypes(),
                    extractParameterNames(method));

            // parameters are null if the method is misconfigured
            if (parameters != null) {
                fields.addAll(parameters);
                builderMethodInjections.add(new MethodInjection(method, parameters));
            }
        }

        // find invalid methods not skipped by findAnnotatedMethods()
        for (Method method : getAllDeclaredMethods(builderClass)) {
            if (method.isAnnotationPresent(ThriftConstructor.class) || hasThriftFieldAnnotation(method)) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    metadataErrors.addError("@ThriftConstructor method [%s] is not public", method.toGenericString());
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    metadataErrors.addError("@ThriftConstructor method [%s] is static", method.toGenericString());
                }
            }
        }

        if (builderMethodInjections.isEmpty()) {
            metadataErrors.addError("Struct builder class [%s] does not have a public builder method annotated with @ThriftConstructor", builderClass.getName());
        }
        if (builderMethodInjections.size() > 1) {
            metadataErrors.addError("Multiple builder methods are annotated with @ThriftConstructor ", builderMethodInjections);
        }
    }

    private void extractFromFields()
    {
        if (builderClass == null) {
            // struct fields are readable and writable
            addFields(structClass, true, true);
        }
        else {
            // builder fields are writable
            addFields(builderClass, false, true);
            // struct fields are readable
            addFields(structClass, true, false);
        }
    }

    private void addFields(Class<?> clazz, boolean allowReaders, boolean allowWriters)
    {
        for (Field fieldField : ReflectionHelper.findAnnotatedFields(clazz, ThriftField.class)) {
            addField(fieldField, allowReaders, allowWriters);
        }

        // find invalid fields not skipped by findAnnotatedFields()
        for (Field field : getAllDeclaredFields(clazz)) {
            if (field.isAnnotationPresent(ThriftField.class)) {
                if (!Modifier.isPublic(field.getModifiers())) {
                    metadataErrors.addError("@ThriftField field [%s] is not public", field.toGenericString());
                }
                if (Modifier.isStatic(field.getModifiers())) {
                    metadataErrors.addError("@ThriftField field [%s] is static", field.toGenericString());
                }
            }
        }
    }

    private void addField(Field fieldField, boolean allowReaders, boolean allowWriters)
    {
        checkArgument(fieldField.isAnnotationPresent(ThriftField.class));

        ThriftField annotation = fieldField.getAnnotation(ThriftField.class);
        if (allowReaders) {
            FieldExtractor fieldExtractor = new FieldExtractor(fieldField, annotation);
            fields.add(fieldExtractor);
            extractors.add(fieldExtractor);
        }
        if (allowWriters) {
            FieldInjection fieldInjection = new FieldInjection(fieldField, annotation);
            fields.add(fieldInjection);
            fieldInjections.add(fieldInjection);
        }
    }

    private void extractFromMethods()
    {
        if (builderClass != null) {
            // builder methods are writable
            addMethods(builderClass, false, true);
            // struct methods are readable
            addMethods(structClass, true, false);
        }
        else {
            // struct methods are readable and writable
            addMethods(structClass, true, true);
        }
    }

    private void addMethods(Class<?> clazz, boolean allowReaders, boolean allowWriters)
    {
        for (Method fieldMethod : findAnnotatedMethods(clazz, ThriftField.class)) {
            addMethod(clazz, fieldMethod, allowReaders, allowWriters);
        }

        // find invalid methods not skipped by findAnnotatedMethods()
        for (Method method : getAllDeclaredMethods(clazz)) {
            if (method.isAnnotationPresent(ThriftField.class) || hasThriftFieldAnnotation(method)) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    metadataErrors.addError("@ThriftField method [%s] is not public", method.toGenericString());
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    metadataErrors.addError("@ThriftField method [%s] is static", method.toGenericString());
                }
            }
        }
    }

    private void addMethod(Class<?> clazz, Method method, boolean allowReaders, boolean allowWriters)
    {
        checkArgument(method.isAnnotationPresent(ThriftField.class));

        ThriftField annotation = method.getAnnotation(ThriftField.class);

        // verify parameters
        if (isValidateGetter(method)) {
            if (allowReaders) {
                MethodExtractor methodExtractor = new MethodExtractor(method, annotation);
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
                            method.getParameterAnnotations(),
                            method.getGenericParameterTypes(),
                            extractParameterNames(method));
                    if (annotation.value() != Short.MIN_VALUE) {
                        metadataErrors.addError("A method with annotated parameters can not have a field id specified: %s.%s ", clazz.getName(), method.getName());
                    }
                    if (!annotation.name().isEmpty()) {
                        metadataErrors.addError("A method with annotated parameters can not have a field name specified: %s.%s ", clazz.getName(), method.getName());
                    }
                    if (annotation.required()) {
                        metadataErrors.addError("A method with annotated parameters can not be marked as required: %s.%s ", clazz.getName(), method.getName());
                    }
                }
                else {
                    parameters = ImmutableList.of(new ParameterInjection(0, annotation, extractFieldName(method), method.getGenericParameterTypes()[0]));
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

    private boolean hasThriftFieldAnnotation(Method method)
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

    private static String extractFieldName(Method method)
    {
        checkNotNull(method, "method is null");
        String methodName = method.getName();
        if ((methodName.startsWith("get") || methodName.startsWith("set")) && methodName.length() > 3) {
            String name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            return name;
        }
        else if (methodName.startsWith("is") && methodName.length() > 2) {
            String name = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            return name;
        }
        else {
            return methodName;
        }

    }

    private boolean isValidateGetter(Method method)
    {
        return method.getParameterTypes().length == 0 && method.getReturnType() != void.class;
    }

    private boolean isValidateSetter(Method method)
    {
        return method.getParameterTypes().length >= 1;
    }

    private List<ParameterInjection> getParameterInjections(Annotation[][] parameterAnnotations, Type[] parameterTypes, String[] parameterNames)
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
                    parameterIndex,
                    thriftField,
                    parameterNames[parameterIndex],
                    parameterType
            );

            parameters.add(parameterInjection);
        }
        return parameters;
    }


    private void normalizeThriftFields(ThriftCatalog catalog)
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
                        metadataErrors.addError("ThriftStruct %s fields %s do not have an id", structName, newTreeSet(transform(fields, getOrExtractThriftFieldName())));
                    }
                }
                continue;
            }
            short fieldId = entry.getKey().get();

            // assure all fields for this ID have the same name
            String fieldName = extractFieldName(fieldId, fields);
            for (FieldMetadata field : fields) {
                field.setName(fieldName);
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
    private Set<String> inferThriftFieldIds()
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

    private void inferThriftFieldIds(Multimap<String, FieldMetadata> fieldsByName, Set<String> fieldsWithConflictingIds)
    {
        // for each name group, set the ids on the fields without ids
        for (Entry<String, Collection<FieldMetadata>> entry : fieldsByName.asMap().entrySet()) {
            Collection<FieldMetadata> fields = entry.getValue();

            // skip all entries without a name or singleton groups... we'll deal with these later
            if (fields.size() <= 1) {
                continue;
            }

            // all ids used by this named field
            Set<Short> ids = ImmutableSet.copyOf(Optional.presentInstances(transform(fields, getThriftFieldId())));

            // multiple conflicting ids
            if (ids.size() > 1) {
                String fieldName = entry.getKey();
                if (!fieldsWithConflictingIds.contains(fieldName)) {
                    metadataErrors.addError("ThriftStruct '%s' field '%s' has multiple ids: %s", structName, fieldName, ids);
                    fieldsWithConflictingIds.add(fieldName);
                }
                continue;
            }

            // single id, so set on all fields in this group (groups with no id are handled later)
            if (ids.size() == 1) {
                // propagate the id to all fields in this group
                short id = Iterables.getOnlyElement(ids);
                for (FieldMetadata field : fields) {
                    field.setId(id);
                }
            }
        }
    }

    private String extractFieldName(short id, Collection<FieldMetadata> fields)
    {
        // get the names used by these fields
        Set<String> names = ImmutableSet.copyOf(filter(transform(fields, getThriftFieldName()), notNull()));

        String name;
        if (!names.isEmpty()) {
            if (names.size() > 1) {
                metadataErrors.addWarning("ThriftStruct %s field %s has multiple names %s", structName, id, names);
            }
            name = names.iterator().next();
        }
        else {
            // pick a name for this field
            name = Iterables.find(transform(fields, extractThriftFieldName()), notNull());
        }
        return name;
    }

    /**
     * Verifies that the the fields all have a supported Java type and that all fields map to the
     * exact same ThriftType.
     */
    private void verifyFieldType(short id, String name, Collection<FieldMetadata> fields, ThriftCatalog catalog)
    {
        boolean isSupportedType = true;
        for (FieldMetadata field : fields) {
            if (!catalog.isSupportedStructFieldType(field.getJavaType())) {
                metadataErrors.addError("ThriftStruct %s field %s(%s) type %s is not a supported Java type", structName, name, id, TypeToken.of(field.getJavaType()));
                isSupportedType = false;
                // only report the error once
                break;
            }
        }

        // fields must have the same type
        if (isSupportedType) {
            Set<ThriftType> types = new HashSet<>();
            for (FieldMetadata field : fields) {
                types.add(catalog.getThriftType(field.getJavaType()));
            }
            if (types.size() > 1) {
                metadataErrors.addWarning("ThriftStruct %s field %s(%s) has multiple types %s", structName, name, id, types);
            }
        }
    }

    //
    // Build final metadata
    //

    public ThriftStructMetadata<T> build()
    {
        // this code assumes that metadata is clean
        metadataErrors.throwIfHasErrors();

        // builder constructor injection
        ThriftMethodInjection builderMethodInjection = buildBuilderConstructorInjections();

        // constructor injection (or factory method for builder)
        ThriftConstructorInjection constructorInjection = buildConstructorInjections();

        // fields injections
        Iterable<ThriftFieldMetadata> fieldsMetadata = buildFieldInjections();

        // methods injections
        List<ThriftMethodInjection> methodInjections = buildMethodInjections();

        return new ThriftStructMetadata<>(
                structName,
                structClass,
                builderClass,
                builderMethodInjection,
                ImmutableList.copyOf(documentation),
                ImmutableList.copyOf(fieldsMetadata),
                constructorInjection,
                methodInjections
        );
    }

    private ThriftMethodInjection buildBuilderConstructorInjections()
    {
        ThriftMethodInjection builderMethodInjection = null;
        if (builderClass != null) {
            MethodInjection builderMethod = builderMethodInjections.get(0);
            builderMethodInjection = new ThriftMethodInjection(builderMethod.getMethod(), buildParameterInjections(builderMethod.getParameters()));
        }
        return builderMethodInjection;
    }

    private ThriftConstructorInjection buildConstructorInjections()
    {
        ConstructorInjection constructor = constructorInjections.get(0);
        return new ThriftConstructorInjection(constructor.getConstructor(), buildParameterInjections(constructor.getParameters()));
    }

    private Iterable<ThriftFieldMetadata> buildFieldInjections()
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

    private ThriftFieldMetadata buildField(Collection<FieldMetadata> input)
    {
        short id = -1;
        String name = null;
        ThriftType type = null;

        // process field injections and extractions
        ImmutableList.Builder<ThriftInjection> injections = ImmutableList.builder();
        ThriftExtraction extraction = null;
        for (FieldMetadata fieldMetadata : input) {
            id = fieldMetadata.getId();
            name = fieldMetadata.getName();
            type = catalog.getThriftType(fieldMetadata.getJavaType());

            if (fieldMetadata instanceof FieldInjection) {
                FieldInjection fieldInjection = (FieldInjection) fieldMetadata;
                injections.add(new ThriftFieldInjection(fieldInjection.getId(), fieldInjection.getName(), fieldInjection.getField()));
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
                extraction = new ThriftFieldExtractor(fieldExtractor.getId(), fieldExtractor.getName(), fieldExtractor.getField());
            }
            else if (fieldMetadata instanceof MethodExtractor) {
                MethodExtractor methodExtractor = (MethodExtractor) fieldMetadata;
                extraction = new ThriftMethodExtractor(methodExtractor.getId(), methodExtractor.getName(), methodExtractor.getMethod());
            }
        }

        // add type coercion
        TypeCoercion coercion = null;
        if (type.isCoerced()) {
            coercion = catalog.getDefaultCoercion(type.getJavaType());
        }

        ThriftFieldMetadata thriftFieldMetadata = new ThriftFieldMetadata(
                id,
                type,
                name,
                injections.build(),
                extraction,
                coercion
        );
        return thriftFieldMetadata;
    }

    private List<ThriftMethodInjection> buildMethodInjections()
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

    private List<ThriftParameterInjection> buildParameterInjections(List<ParameterInjection> parameters)
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

    static abstract class FieldMetadata
    {
        private Short id;
        private String name;

        private FieldMetadata(ThriftField annotation)
        {
            if (annotation != null) {
                if (annotation.value() != Short.MIN_VALUE) {
                    id = annotation.value();
                }
                if (!annotation.name().isEmpty()) {
                    name = annotation.name();
                }
            }
        }

        public Short getId()
        {
            return id;
        }

        public void setId(short id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public abstract Type getJavaType();

        public abstract String extractName();

        static <T extends FieldMetadata> Function<T, Optional<Short>> getThriftFieldId()
        {
            return new Function<T, Optional<Short>>()
            {
                @Override
                public Optional<Short> apply(@Nullable T input)
                {
                    if (input == null) {
                        return Optional.absent();
                    }
                    Short value = input.getId();
                    return Optional.fromNullable(value);
                }
            };
        }

        static <T extends FieldMetadata> Function<T, String> getThriftFieldName()
        {
            return new Function<T, String>()
            {
                @Override
                public String apply(@Nullable T input)
                {
                    if (input == null) {
                        return null;
                    }
                    return input.getName();
                }
            };
        }

        static <T extends FieldMetadata> Function<T, String> getOrExtractThriftFieldName()
        {
            return new Function<T, String>()
            {
                @Override
                public String apply(@Nullable T input)
                {
                    if (input == null) {
                        return null;
                    }
                    String name = input.getName();
                    if (name == null) {
                        name = input.extractName();
                    }
                    if (name == null) {
                        throw new NullPointerException(String.valueOf("name is null"));
                    }
                    return name;
                }
            };
        }

        static <T extends FieldMetadata> Function<T, String> extractThriftFieldName()
        {
            return new Function<T, String>()
            {
                @Override
                public String apply(@Nullable T input)
                {
                    if (input == null) {
                        return null;
                    }
                    return input.extractName();
                }
            };
        }
    }

    private static abstract class Extractor extends FieldMetadata
    {
        protected Extractor(ThriftField annotation)
        {
            super(annotation);
        }
    }

    private static class FieldExtractor extends Extractor
    {
        private final Field field;

        private FieldExtractor(Field field, ThriftField annotation)
        {
            super(annotation);
            this.field = field;
        }

        public Field getField()
        {
            return field;
        }

        @Override
        public String extractName()
        {
            return field.getName();
        }

        @Override
        public Type getJavaType()
        {
            return field.getGenericType();
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("FieldExtractor");
            sb.append("{field=").append(field);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class MethodExtractor extends Extractor
    {
        private final Method method;

        public MethodExtractor(Method method, ThriftField annotation)
        {
            super(annotation);
            this.method = method;
        }

        public Method getMethod()
        {
            return method;
        }

        @Override
        public String extractName()
        {
            return extractFieldName(method);
        }

        @Override
        public Type getJavaType()
        {
            return method.getGenericReturnType();
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("MethodExtractor");
            sb.append("{method=").append(method);
            sb.append('}');
            return sb.toString();
        }
    }

    private static abstract class Injection extends FieldMetadata
    {
        protected Injection(ThriftField annotation)
        {
            super(annotation);
        }
    }

    private static class FieldInjection extends Injection
    {
        private final Field field;

        private FieldInjection(Field field, ThriftField annotation)
        {
            super(annotation);
            this.field = field;
        }

        public Field getField()
        {
            return field;
        }

        @Override
        public String extractName()
        {
            return field.getName();
        }

        @Override
        public Type getJavaType()
        {
            return field.getGenericType();
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("FieldInjection");
            sb.append("{field=").append(field);
            sb.append('}');
            return sb.toString();
        }
    }

    public class ConstructorInjection
    {
        private final Constructor<?> constructor;
        private final List<ParameterInjection> parameters;

        public ConstructorInjection(Constructor<?> constructor, List<ParameterInjection> parameters)
        {
            this.constructor = constructor;
            this.parameters = ImmutableList.copyOf(parameters);
        }

        public ConstructorInjection(Constructor<?> constructor, ParameterInjection... parameters)
        {
            this.constructor = constructor;
            this.parameters = ImmutableList.copyOf(parameters);
        }

        public Constructor<?> getConstructor()
        {
            return constructor;
        }

        public List<ParameterInjection> getParameters()
        {
            return parameters;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("ConstructorInjection");
            sb.append("{constructor=").append(constructor);
            sb.append(", parameters=").append(parameters);
            sb.append('}');
            return sb.toString();
        }
    }

    public class MethodInjection
    {
        private final Method method;
        private final List<ParameterInjection> parameters;

        public MethodInjection(Method method, List<ParameterInjection> parameters)
        {
            this.method = method;
            this.parameters = ImmutableList.copyOf(parameters);
        }

        public Method getMethod()
        {
            return method;
        }

        public List<ParameterInjection> getParameters()
        {
            return parameters;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("MethodInjection");
            sb.append("{method=").append(method);
            sb.append(", parameters=").append(parameters);
            sb.append('}');
            return sb.toString();
        }
    }

    private static class ParameterInjection extends Injection
    {
        private final int parameterIndex;
        private final String extractedName;
        private final Type parameterJavaType;

        private ParameterInjection(int parameterIndex, ThriftField annotation, String extractedName, Type parameterJavaType)
        {
            super(annotation);
            checkNotNull(parameterJavaType, "parameterJavaType is null");

            this.parameterIndex = parameterIndex;
            this.extractedName = extractedName;
            this.parameterJavaType = parameterJavaType;
            if (void.class.equals(parameterJavaType)) {
                throw new AssertionError();
            }
            checkArgument(getName() != null || extractedName != null, "Parameter must have an explicit name or an extractedName");
        }

        public int getParameterIndex()
        {
            return parameterIndex;
        }

        @Override
        public String extractName()
        {
            return extractedName;
        }

        @Override
        public Type getJavaType()
        {
            return parameterJavaType;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("ParameterInjection");
            sb.append("{parameterIndex=").append(parameterIndex);
            sb.append(", extractedName='").append(extractedName).append('\'');
            sb.append(", parameterJavaType=").append(parameterJavaType);
            sb.append('}');
            return sb.toString();
        }
    }
}
