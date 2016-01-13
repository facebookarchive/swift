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

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftIdlAnnotation;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.Map;

import static com.facebook.swift.codec.ThriftField.RECURSIVE_REFERENCE_ANNOTATION_NAME;
import static com.facebook.swift.codec.ThriftField.Requiredness;
import static com.google.common.base.Preconditions.checkNotNull;

abstract class FieldMetadata
{
    private Short id;
    private Boolean isLegacyId;
    private Boolean isRecursiveReference;
    private String name;
    private Requiredness requiredness;
    private Map<String, String> idlAnnotations;
    private final FieldKind type;

    protected FieldMetadata(ThriftField annotation, FieldKind type)
    {
        this.type = type;

        switch (type) {
            case THRIFT_FIELD:
                if (annotation != null) {
                    if (annotation.value() != Short.MIN_VALUE) {
                        id = annotation.value();
                    }
                    isLegacyId = annotation.isLegacyId();
                    if (!annotation.name().isEmpty()) {
                        name = annotation.name();
                    }
                    requiredness = checkNotNull(annotation.requiredness());

                    ImmutableMap.Builder<String, String> annotationMapBuilder = ImmutableMap.builder();
                    for (ThriftIdlAnnotation idlAnnotation : annotation.idlAnnotations()) {
                        annotationMapBuilder.put(idlAnnotation.key(), idlAnnotation.value());
                    }
                    idlAnnotations = annotationMapBuilder.build();

                    if (annotation.isRecursive() != ThriftField.Recursiveness.UNSPECIFIED) {
                        switch (annotation.isRecursive()) {
                            case TRUE:
                                isRecursiveReference = true;
                                break;
                            case FALSE:
                                isRecursiveReference = false;
                                break;
                            default:
                                throw new IllegalStateException("Unexpected get for isRecursive field");
                        }
                    }
                    else if (idlAnnotations.containsKey(RECURSIVE_REFERENCE_ANNOTATION_NAME)) {
                        isRecursiveReference = "true".equalsIgnoreCase(idlAnnotations.getOrDefault(RECURSIVE_REFERENCE_ANNOTATION_NAME, "false"));
                    }
                }
                break;
            case THRIFT_UNION_ID:
                assert annotation == null : "ThriftStruct annotation shouldn't be present for THRIFT_UNION_ID";
                id = Short.MIN_VALUE;
                isLegacyId = true; // preserve `negative field ID <=> isLegacyId`
                name = "_union_id";
                break;
            default:
                throw new IllegalArgumentException("Encountered field metadata type " + type);
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

    public @Nullable Boolean isLegacyId()
    {
        return isLegacyId;
    }

    public void setIsLegacyId(Boolean isLegacyId)
    {
        this.isLegacyId = isLegacyId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Map<String, String> getIdlAnnotations()
    {
        return idlAnnotations;
    }

    public void setIdlAnnotations(Map<String, String> idlAnnotations)
    {
        this.idlAnnotations = idlAnnotations;
    }

    public FieldKind getType()
    {
        return type;
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

    /**
     * Returns a Function which gets the `isLegacyId` setting from a FieldMetadata, if present,
     * or {@link Optional#absent()} if not, ish.
     *
     * The semantics would ideally want are:
     * <pre>
     *     1   @ThriftField(id=X, isLegacyId=false)   => Optional.of(false)
     *     2   @ThriftField(id=X, isLegacyId=true)    => Optional.of(true)
     *     3   @ThriftField(isLegacyId=false)         => Optional.of(false)
     *     4   @ThriftField(isLegacyId=true)          => Optional.of(true)
     *     5   @ThriftField()                         => Optional.absent()
     * </pre>
     *
     * Unfortunately, there is no way to tell cases 3 and 5 apart, because isLegacyId
     * defaults to false. (There is no good way around this: making an enum is overkill,
     * using a numeric/character/string/class type is pretty undesirable, and requiring
     * isLegacyId to be specified explicitly on every ThriftField is unacceptable.)
     * The best we can do is treat 3 and 5 the same (obviously needing the behavior
     * of 5.) This ends up actually not making much of a difference: it would fail to
     * detect cases like:
     *
     * <pre>
     *   @ThriftField(id=-2, isLegacyId=true)
     *   public boolean getBlah() { ... }
     *
     *   @ThriftField(isLegacyId=false)
     *   public void setBlah(boolean v) { ...}
     * </pre>
     *
     * but other than that, ends up working out fine.
     */
    static <T extends FieldMetadata> Function<T, Optional<Boolean>> getThriftFieldIsLegacyId()
    {
        return new Function<T, Optional<Boolean>>()
        {
            @Override
            public Optional<Boolean> apply(@Nullable T input)
            {
                if (input == null) {
                    return Optional.absent();
                }
                Boolean value = input.isLegacyId();

                if (input.getId() == null || input.getId().shortValue() == Short.MIN_VALUE) {
                    if (value != null && value.booleanValue() == false) {
                        return Optional.absent();
                    }
                }

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

    static <T extends FieldMetadata> Function<T, Requiredness> getThriftFieldRequiredness()
    {
        return new Function<T, Requiredness>()
        {
            @Nullable
            @Override
            public Requiredness apply(@Nullable T input)
            {
                return input.getRequiredness();
            }
        };
    }

    public Requiredness getRequiredness()
    {
        return requiredness;
    }

    public void setRequiredness(Requiredness requiredness)
    {
        this.requiredness = requiredness;
    }

    public @Nullable Boolean isRecursiveReference()
    {
        return isRecursiveReference;
    }

    public void setIsRecursiveReference(Boolean isRecursiveReference)
    {
        this.isRecursiveReference = isRecursiveReference;
    }
}
