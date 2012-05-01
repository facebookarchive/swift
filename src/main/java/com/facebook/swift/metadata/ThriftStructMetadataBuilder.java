/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.metadata;

import com.facebook.swift.ThriftConstructor;
import com.facebook.swift.ThriftField;
import com.facebook.swift.ThriftProtocolFieldType;
import com.facebook.swift.ThriftStruct;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import static com.facebook.swift.metadata.ReflectionHelper.findAnnotatedMethods;
import static com.facebook.swift.metadata.ReflectionHelper.getAllDeclaredFields;
import static com.facebook.swift.metadata.ReflectionHelper.getAllDeclaredMethods;
import static com.facebook.swift.metadata.ThriftStructMetadataBuilder.FieldMetadata.extractThriftFieldName;
import static com.facebook.swift.metadata.ThriftStructMetadataBuilder.FieldMetadata.getOrExtractThriftFieldName;
import static com.facebook.swift.metadata.ThriftStructMetadataBuilder.FieldMetadata.getThriftFieldId;
import static com.facebook.swift.metadata.ThriftStructMetadataBuilder.FieldMetadata.getThriftFieldName;
import static com.facebook.swift.metadata.ThriftStructMetadataBuilder.FieldMetadata.getThriftFieldProtocolType;
import static com.facebook.swift.metadata.ThriftStructMetadataBuilder.FieldMetadata.getThriftFieldType;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static java.util.Arrays.asList;

public class ThriftStructMetadataBuilder<T> {
  private final String structName;
  private final Class<T> structClass;
  private final Class<?> builderClass;

  private final List<FieldMetadata> fields = newArrayList();

  // readers
  private final List<Extractor> extractors = newArrayList();

  // writers
  private final List<MethodInjection> builderMethodInjections = newArrayList();
  private final List<ConstructorInjection> constructorInjections = newArrayList();
  private final List<FieldInjection> fieldInjections = newArrayList();
  private final List<MethodInjection> methodInjections = newArrayList();

  private final ThriftCatalog catalog;
  private final Problems problems;

  public ThriftStructMetadataBuilder(ThriftCatalog catalog, Class<T> structClass) {
    this.catalog = checkNotNull(catalog, "catalog is null");
    this.structClass = checkNotNull(structClass, "structClass is null");
    this.problems = new Problems(catalog.getMonitor());

    // Verify struct class is public and not abstract
    if (Modifier.isAbstract(structClass.getModifiers())) {
      problems.addError("Struct class [%s] is abstract", structClass.getName());
    }
    if (!Modifier.isPublic(structClass.getModifiers())) {
      problems.addError("Struct class [%s] is not public", structClass.getName());
    }

    // name
    ThriftStruct annotation = structClass.getAnnotation(ThriftStruct.class);
    if (annotation != null) {
      if (!annotation.name().isEmpty()) {
        structName = annotation.name();
      } else {
        structName = structClass.getSimpleName();
      }
    } else {
      problems.addError(
        "Struct class [%s] does not have a @ThriftStruct annotation",
        structClass.getName()
      );
      structName = structClass.getSimpleName();
    }

    // builder class
    if (!annotation.builder().equals(void.class)) {
      builderClass = annotation.builder();
    } else {
      builderClass = null;
    }

    // constructors
    if (builderClass != null) {
      // builder class must have a valid constructor
      addConstructors(builderClass);

      // builder class must have a build method annotated with @ThriftConstructor
      addBuilderMethods();

      // verify struct class does not have @ThriftConstructors
      for (Constructor<?> constructor : structClass.getConstructors()) {
        if (constructor.isAnnotationPresent(ThriftConstructor.class)) {
          problems.addWarning(
            "Struct class [%s] has a builder class, but constructor %s annotated with @ThriftConstructor",
            structClass.getName(),
            constructor
          );
        }
      }
    } else {
      // struct class must have a valid constructor
      addConstructors(structClass);
    }

    // fields
    if (builderClass != null) {
      addFields(builderClass, false, true);
      addFields(structClass, true, false);
    } else {
      addFields(structClass, true, true);
    }

    // methods
    if (builderClass != null) {
      addMethods(builderClass, false, true);
      addMethods(structClass, true, false);
    } else {
      addMethods(structClass, true, true);
    }

    // group fields by explicit name or by name extracted from field, method or property
    Multimap<String, FieldMetadata> fieldsByName = Multimaps.index(
      fields,
      getOrExtractThriftFieldName()
    );

    // for each name group set the ids on the fields without ids
    for (Entry<String, Collection<FieldMetadata>> entry : fieldsByName.asMap().entrySet()) {
      Collection<FieldMetadata> fields = entry.getValue();

      // skip all entries without a name or singleton groups... we'll deal with these below
      if (fields.size() <= 1) {
        continue;
      }

      short id = Iterables.find(transform(fields, getThriftFieldId()), notNull(), Short.MIN_VALUE);
      if (id != Short.MIN_VALUE) {
        for (FieldMetadata field : fields) {
          field.setId(id);
        }
      }
    }

    // group fields by id
    Multimap<Short, FieldMetadata> fieldsById = Multimaps.index(fields, getThriftFieldId());
    for (Entry<Short, Collection<FieldMetadata>> entry : fieldsById.asMap().entrySet()) {
      Short id = entry.getKey();
      Collection<FieldMetadata> fields = entry.getValue();

      // fields must have an id
      if (id == null) {
        for (FieldMetadata field : fields) {
          problems.addError(
            "ThriftStruct %s field %s, does not have an id",
            structName,
            field.getName()
          ); // todo bad message
        }
        continue;
      }

      // assure all fields with the same id hav the same name
      Set<String> names = ImmutableSet.copyOf(
        filter(
          transform(fields, getThriftFieldName()),
          notNull()
        )
      );
      String name;
      if (!names.isEmpty()) {
        if (names.size() > 1) {
          problems.addWarning("Field %s has multiple names %s", id, names);
        }
        name = names.iterator().next();
      } else {
        // pick a name for this field
        name = Iterables.find(transform(fields, extractThriftFieldName()), notNull());
      }
      for (FieldMetadata field : fields) {
        field.setName(name);
      }

      // assure all fields have the same protocol type
      Set<ThriftProtocolFieldType> protocolTypes = ImmutableSet.copyOf(
        filter(
          transform(
            fields,
            getThriftFieldProtocolType()
          ), notNull()
        )
      );
      if (protocolTypes.size() > 1) {
        // todo allow primitive widening and narrowing conversions
        problems.addError("Field %s has multiple conflicting protocol types %s", id, protocolTypes);
        continue;
      }
      ThriftProtocolFieldType type = protocolTypes.iterator().next();
      for (FieldMetadata field : fields) {
        field.setProtocolType(type);
      }

      // assure all fields have the same compatible thrift types
      Set<ThriftType> types = ImmutableSet.copyOf(
        filter(transform(fields, getThriftFieldType(catalog)), notNull())
      );
      if (!types.isEmpty()) {
        if (protocolTypes.size() > 1) {
          problems.addError("Field %s has multiple conflicting thrift types %s", id, protocolTypes);
        }
      }
    }
  }

  public Problems getProblems() {
    return problems;
  }

  public ThriftStructMetadata<T> build() {
    // we can only build if there are no errors
    problems.throwIfHasErrors();

    ThriftMethodInjection builderMethodInjection = null;
    if (builderClass != null) {
      MethodInjection builderMethod = builderMethodInjections.get(0);
      builderMethodInjection = new ThriftMethodInjection(
        builderMethod.getMethod(),
        toThriftParameterInjections(builderMethod.getParameters())
      );
    }
    ConstructorInjection constructor = constructorInjections.get(0);

    Multimap<Short, FieldMetadata> fieldsById = Multimaps.index(fields, getThriftFieldId());
    Iterable<ThriftFieldMetadata> fieldsMetadata = Iterables.transform(
      fieldsById.asMap().values(), new Function<Collection<FieldMetadata>, ThriftFieldMetadata>() {
      @Override
      public ThriftFieldMetadata apply(Collection<FieldMetadata> input) {
        checkArgument(!input.isEmpty(), "input is empty");

        short id = -1;
        String name = null;
        ThriftType type = null;
        ImmutableList.Builder<ThriftInjection> injections = ImmutableList.builder();
        ThriftExtraction extraction = null;
        for (FieldMetadata fieldMetadata : input) {
          id = fieldMetadata.getId();
          name = fieldMetadata.getName();
          type = catalog.getThriftType(fieldMetadata.getJavaType(), fieldMetadata.getProtocolType());
          if (fieldMetadata instanceof FieldInjection) {
            FieldInjection fieldInjection = (FieldInjection) fieldMetadata;
            injections.add(
              new ThriftFieldInjection(
                fieldInjection.getId(),
                fieldInjection.getName(),
                fieldInjection.getField()
              )
            );
          } else if (fieldMetadata instanceof ParameterInjection) {
            ParameterInjection parameterInjection = (ParameterInjection) fieldMetadata;
            injections.add(
              new ThriftParameterInjection(
                parameterInjection.getId(),
                parameterInjection.getName(),
                parameterInjection.getParameterIndex()
              )
            );
          } else if (fieldMetadata instanceof FieldExtractor) {
            FieldExtractor fieldExtractor = (FieldExtractor) fieldMetadata;
            extraction = new ThriftFieldExtractor(
              fieldExtractor.getId(),
              fieldExtractor.getName(),
              fieldExtractor.getField()
            );
          } else if (fieldMetadata instanceof MethodExtractor) {
            MethodExtractor methodExtractor = (MethodExtractor) fieldMetadata;
            extraction = new ThriftMethodExtractor(
              methodExtractor.getId(),
              methodExtractor.getName(),
              methodExtractor.getMethod()
            );
          }
        }
        ThriftFieldMetadata thriftFieldMetadata = new ThriftFieldMetadata(
          id,
          type,
          name,
          injections.build(),
          extraction
        );
        return thriftFieldMetadata;
      }
    }
    );

    return new ThriftStructMetadata<>(
      structName,
      structClass,
      builderClass,
      builderMethodInjection,
      ImmutableList.copyOf(fieldsMetadata),
      new ThriftConstructorInjection(
        constructor.getConstructor(),
        toThriftParameterInjections(constructor.getParameters())
      ),
      toThriftMethodInjections(methodInjections)
    );
  }

  private List<ThriftMethodInjection> toThriftMethodInjections(List<MethodInjection> methodInjections) {
    return Lists.transform(
      methodInjections, new Function<MethodInjection, ThriftMethodInjection>() {
      @Override
      public ThriftMethodInjection apply(@Nullable MethodInjection input) {
        return new ThriftMethodInjection(
          input.getMethod(),
          toThriftParameterInjections(input.getParameters())
        );
      }
    }
    );
  }

  private List<ThriftParameterInjection> toThriftParameterInjections(List<ParameterInjection> parameters) {
    return Lists.transform(
      parameters, new Function<ParameterInjection, ThriftParameterInjection>() {
      @Override
      public ThriftParameterInjection apply(@Nullable ParameterInjection input) {
        return new ThriftParameterInjection(
          input.getId(),
          input.getName(),
          input.getParameterIndex()
        );
      }
    }
    );
  }

  private void addBuilderMethods() {
    for (Method method : findAnnotatedMethods(builderClass, ThriftConstructor.class)) {
      List<ParameterInjection> parameters = getParameterInjections(
        method.toGenericString(),
        method.getParameterAnnotations(),
        method.getGenericParameterTypes()
      );
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
          problems.addError(
            "@ThriftConstructor method [%s] is not public",
            method.toGenericString()
          );
        }
        if (Modifier.isStatic(method.getModifiers())) {
          problems.addError("@ThriftConstructor method [%s] is static", method.toGenericString());
        }
      }
    }

    if (builderMethodInjections.isEmpty()) {
      problems.addError(
        "Struct builder class [%s] does not have a public builder method annotated with @ThriftConstructor",
        builderClass.getName()
      );
    }
    if (builderMethodInjections.size() > 1) {
      problems.addError(
        "Multiple builder methods are annotated with @ThriftConstructor ",
        builderMethodInjections
      );
    }
  }

  private void addConstructors(Class<?> clazz) {
    for (Constructor<?> constructor : clazz.getConstructors()) {
      if (constructor.isSynthetic()) {
        continue;
      }
      if (!constructor.isAnnotationPresent(ThriftConstructor.class)) {
        continue;
      }

      if (!Modifier.isPublic(constructor.getModifiers())) {
        problems.addError("@ThriftConstructor [%s] is not public", constructor.toGenericString());
        continue;
      }

      List<ParameterInjection> parameters = getParameterInjections(
        constructor.toGenericString(),
        constructor.getParameterAnnotations(),
        constructor.getGenericParameterTypes()
      );
      if (parameters != null) {
        fields.addAll(parameters);
        constructorInjections.add(new ConstructorInjection(constructor, parameters));
      }
    }

    // add the default constructor
    if (constructorInjections.isEmpty()) {
      // todo look for builder class
      try {
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        if (!Modifier.isPublic(constructor.getModifiers())) {
          problems.addError(
            "Default constructor [%s] is not public",
            constructor.toGenericString()
          );
        }
        constructorInjections.add(new ConstructorInjection(constructor));
      } catch (Exception e) {
        problems.addError(
          "Struct class [%s] does not have a public no-arg constructor",
          clazz.getName()
        );
      }
    }

    if (constructorInjections.size() > 1) {
      problems.addError(
        "Multiple constructors are annotated with @ThriftConstructor ",
        constructorInjections
      );
    }
  }

  private void addFields(Class<?> clazz, boolean allowReaders, boolean allowWriters) {
    for (Field fieldField : ReflectionHelper.findAnnotatedFields(clazz, ThriftField.class)) {
      addField(fieldField, allowReaders, allowWriters);
    }

    // find invalid fields not skipped by findAnnotatedFields()
    for (Field field : getAllDeclaredFields(clazz)) {
      if (field.isAnnotationPresent(ThriftField.class)) {
        if (!Modifier.isPublic(field.getModifiers())) {
          problems.addError("@ThriftField field [%s] is not public", field.toGenericString());
        }
        if (Modifier.isStatic(field.getModifiers())) {
          problems.addError("@ThriftField field [%s] is static", field.toGenericString());
        }
      }
    }
  }

  private void addField(Field fieldField, boolean allowReaders, boolean allowWriters) {
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

  private void addMethods(Class<?> clazz, boolean allowReaders, boolean allowWriters) {
    for (Method fieldMethod : findAnnotatedMethods(clazz, ThriftField.class)) {
      addMethod(clazz, fieldMethod, allowReaders, allowWriters);
    }

    // find invalid methods not skipped by findAnnotatedMethods()
    for (Method method : getAllDeclaredMethods(clazz)) {
      if (method.isAnnotationPresent(ThriftField.class) || hasThriftFieldAnnotation(method)) {
        if (!Modifier.isPublic(method.getModifiers())) {
          problems.addError("@ThriftField method [%s] is not public", method.toGenericString());
        }
        if (Modifier.isStatic(method.getModifiers())) {
          problems.addError("@ThriftField method [%s] is static", method.toGenericString());
        }
      }
    }
  }

  private void addMethod(
    Class<?> clazz,
    Method method,
    boolean allowReaders,
    boolean allowWriters
  ) {
    checkArgument(method.isAnnotationPresent(ThriftField.class));

    ThriftField annotation = method.getAnnotation(ThriftField.class);

    // verify parameters
    if (isValidateGetter(method)) {
      if (allowReaders) {
        MethodExtractor methodExtractor = new MethodExtractor(method, annotation);
        fields.add(methodExtractor);
        extractors.add(methodExtractor);
      } else {
        problems.addError(
          "Reader method %s.%s is not allowed on a builder class",
          clazz.getName(),
          method.getName()
        );
      }
    } else if (isValidateSetter(method)) {
      if (allowWriters) {
        List<ParameterInjection> parameters;
        if (method.getParameterTypes().length > 1 || Iterables.any(
          asList(method.getParameterAnnotations()[0]),
          Predicates.instanceOf(ThriftField.class)
        )) {
          parameters = getParameterInjections(
            method.toGenericString(),
            method.getParameterAnnotations(),
            method.getGenericParameterTypes()
          );
          if (annotation.id() != Short.MIN_VALUE) {
            problems.addError(
              "A method with annotated parameters can not have a field id specified: %s.%s ",
              clazz.getName(),
              method.getName()
            );
          }
          if (!annotation.name().isEmpty()) {
            problems.addError(
              "A method with annotated parameters can not have a field name specified: %s.%s ",
              clazz.getName(),
              method.getName()
            );
          }
          if (annotation.protocolType() != ThriftProtocolFieldType.STOP) {
            problems.addError(
              "A method with annotated parameters can not have a field type specified: %s.%s ",
              clazz.getName(),
              method.getName()
            );
          }
          if (annotation.required()) {
            problems.addError(
              "A method with annotated parameters can not be marked as required: %s.%s ",
              clazz.getName(),
              method.getName()
            );
          }
        } else {
          parameters = ImmutableList.of(
            new ParameterInjection(
              0, annotation, extractFieldName(
              method
            ), method.getGenericParameterTypes()[0]
            )
          );
        }
        fields.addAll(parameters);
        methodInjections.add(new MethodInjection(method, parameters));
      } else {
        problems.addError(
          "Inject method %s.%s is not allowed on struct class, since struct has a builder",
          clazz.getName(),
          method.getName()
        );
      }
    } else {
      problems.addError(
        "Method %s.%s is not a supported getter or setter",
        clazz.getName(),
        method.getName()
      );
    }
  }

  private boolean hasThriftFieldAnnotation(Method method) {
    for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
      for (Annotation parameterAnnotation : parameterAnnotations) {
        if (parameterAnnotation instanceof ThriftField) {
          return true;
        }
      }
    }
    return false;
  }

  private List<ParameterInjection> getParameterInjections(
    String methodSignature,
    Annotation[][] parameterAnnotations,
    Type[] parameterTypes
  ) {
    boolean invalid = false;

    List<ParameterInjection> parameters = newArrayListWithCapacity(parameterAnnotations.length);
    for (int parameterIndex = 0; parameterIndex < parameterAnnotations.length; parameterIndex++) {
      Annotation[] annotations = parameterAnnotations[parameterIndex];
      Type parameterType = parameterTypes[parameterIndex];
      for (Annotation annotation : annotations) {
        if (!(annotation instanceof ThriftField)) {
          invalid = true;
          continue;
        }

        ThriftField thriftField = (ThriftField) annotation;

        ParameterInjection parameterInjection = new ParameterInjection(
          parameterIndex,
          thriftField,
          "arg" + parameterIndex,
          parameterType
        );

        // verify either id or name is set
        // todo add name discovery
        if (parameterInjection.getId() == null && parameterInjection.getName() == null) {
          problems.addError(
            "@ThriftConstructor %s parameter %s does not have name or id specified",
            methodSignature,
            parameterIndex
          );
          invalid = true;
          continue;
        }

        parameters.add(parameterInjection);
      }
    }
    if (invalid) {
      return null;
    }
    return parameters;
  }

  private boolean isValidateGetter(Method method) {
    return method.getParameterTypes().length == 0 && method.getReturnType() != void.class;
  }

  private boolean isValidateSetter(Method method) {
    return method.getParameterTypes().length >= 1;
  }

  private static String extractFieldName(Method method) {
    checkNotNull(method, "method is null");
    String methodName = method.getName();
    if ((methodName.startsWith("get") || methodName.startsWith("set")) && methodName.length() > 3) {
      String name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
      return name;
    } else if (methodName.startsWith("is") && methodName.length() > 2) {
      String name = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
      return name;
    } else {
      return methodName;
    }

  }

  static abstract class FieldMetadata {
    private Short id;
    private String name;
    private ThriftProtocolFieldType protocolType;

    private FieldMetadata(ThriftField annotation) {
      checkNotNull(annotation, "annotation is null");
      if (annotation.id() != Short.MIN_VALUE) {
        id = annotation.id();
      }
      if (!annotation.name().isEmpty()) {
        name = annotation.name();
      }
      if (annotation.protocolType() != ThriftProtocolFieldType.STOP) {
        protocolType = annotation.protocolType();
      }
    }

    public Short getId() {
      return id;
    }

    public void setId(short id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public abstract Type getJavaType();

    public abstract String extractName();

    public ThriftProtocolFieldType getProtocolType() {
      return protocolType;
    }

    public void setProtocolType(ThriftProtocolFieldType protocolType) {
      this.protocolType = protocolType;
    }

    static <T extends FieldMetadata> Function<T, Short> getThriftFieldId() {
      return new Function<T, Short>() {
        @Override
        public Short apply(@Nullable T input) {
          if (input == null) {
            return null;
          }
          return input.getId();
        }
      };
    }

    static <T extends FieldMetadata> Function<T, String> getThriftFieldName() {
      return new Function<T, String>() {
        @Override
        public String apply(@Nullable T input) {
          if (input == null) {
            return null;
          }
          return input.getName();
        }
      };
    }

    static <T extends FieldMetadata> Function<T, ThriftProtocolFieldType> getThriftFieldProtocolType() {
      return new Function<T, ThriftProtocolFieldType>() {
        @Override
        public ThriftProtocolFieldType apply(@Nullable T input) {
          if (input == null) {
            return null;
          }
          ThriftProtocolFieldType protocolType = input.getProtocolType();
          if (protocolType == null) {
            Type javaType = input.getJavaType();
            protocolType = ThriftProtocolFieldType.inferProtocolType(javaType);
          }
          return protocolType;
        }
      };
    }

    static <T extends FieldMetadata> Function<T, ThriftType> getThriftFieldType(final ThriftCatalog catalog) {
      return new Function<T, ThriftType>() {
        @Override
        public ThriftType apply(@Nullable T input) {
          if (input == null) {
            return null;
          }
          ThriftProtocolFieldType protocolType = input.getProtocolType();
          Preconditions.checkNotNull(
            protocolType,
            "protocolType is null for field %s",
            input.getName()
          );
          Type javaType = input.getJavaType();
          Preconditions.checkNotNull(javaType, "javaType is null for field %s", input.getName());
          ThriftType thriftType = catalog.getThriftType(javaType, protocolType);
          return thriftType;
        }
      };
    }

    static <T extends FieldMetadata> Function<T, String> getOrExtractThriftFieldName() {
      return new Function<T, String>() {
        @Override
        public String apply(@Nullable T input) {
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

    static <T extends FieldMetadata> Function<T, String> extractThriftFieldName() {
      return new Function<T, String>() {
        @Override
        public String apply(@Nullable T input) {
          if (input == null) {
            return null;
          }
          return input.extractName();
        }
      };
    }
  }

  private static abstract class Extractor extends FieldMetadata {
    protected Extractor(ThriftField annotation) {
      super(annotation);
    }
  }

  private static class FieldExtractor extends Extractor {
    private final Field field;

    private FieldExtractor(Field field, ThriftField annotation) {
      super(annotation);
      this.field = field;
    }

    public Field getField() {
      return field;
    }

    @Override
    public String extractName() {
      return field.getName();
    }

    @Override
    public Type getJavaType() {
      return field.getGenericType();
    }
  }

  public static class MethodExtractor extends Extractor {
    private final Method method;

    public MethodExtractor(Method method, ThriftField annotation) {
      super(annotation);
      this.method = method;
    }

    public Method getMethod() {
      return method;
    }

    @Override
    public String extractName() {
      return extractFieldName(method);
    }

    @Override
    public Type getJavaType() {
      return method.getGenericReturnType();
    }
  }

  private static class FieldInjection extends FieldMetadata {
    private final Field field;

    private FieldInjection(Field field, ThriftField annotation) {
      super(annotation);
      this.field = field;
    }

    public Field getField() {
      return field;
    }

    @Override
    public String extractName() {
      return field.getName();
    }

    @Override
    public Type getJavaType() {
      return field.getGenericType();
    }
  }

  public class ConstructorInjection {
    private final Constructor<?> constructor;
    private final List<ParameterInjection> parameters;

    public ConstructorInjection(Constructor<?> constructor, List<ParameterInjection> parameters) {
      this.constructor = constructor;
      this.parameters = ImmutableList.copyOf(parameters);
    }

    public ConstructorInjection(Constructor<?> constructor, ParameterInjection... parameters) {
      this.constructor = constructor;
      this.parameters = ImmutableList.copyOf(parameters);
    }

    public Constructor<?> getConstructor() {
      return constructor;
    }

    public List<ParameterInjection> getParameters() {
      return parameters;
    }
  }

  public class MethodInjection {
    private final Method method;
    private final List<ParameterInjection> parameters;

    public MethodInjection(Method method, List<ParameterInjection> parameters) {
      this.method = method;
      this.parameters = ImmutableList.copyOf(parameters);
    }

    public Method getMethod() {
      return method;
    }

    public List<ParameterInjection> getParameters() {
      return parameters;
    }
  }

  private static class ParameterInjection extends FieldMetadata {
    private final int parameterIndex;
    private final String extractedName;
    private final Type parameterJavaType;

    private ParameterInjection(
      int parameterIndex,
      ThriftField annotation,
      String extractedName,
      Type parameterJavaType
    ) {
      super(annotation);
      this.parameterIndex = parameterIndex;
      this.extractedName = extractedName;
      this.parameterJavaType = Preconditions.checkNotNull(
        parameterJavaType,
        "parameterJavaType is null"
      );
      if (void.class.equals(parameterJavaType)) {
        throw new AssertionError();
      }
      checkArgument(
        getName() != null || extractedName != null,
        "Parameter must have an explicit name or an extractedName"
      );
    }

    public int getParameterIndex() {
      return parameterIndex;
    }

    @Override
    public String extractName() {
      return extractedName;
    }

    @Override
    public Type getJavaType() {
      return parameterJavaType;
    }
  }
}
