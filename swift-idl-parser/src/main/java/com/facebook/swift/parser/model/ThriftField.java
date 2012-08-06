package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThriftField
{
    public static enum Required
    {
        REQUIRED, OPTIONAL, NONE
    }

    private final String name;
    private final ThriftType type;
    private final Optional<Long> identifier;
    private final Required required;
    private final Optional<ConstValue> value;
    private final List<TypeAnnotation> annotations;

    public ThriftField(
            String name,
            ThriftType type,
            Long identifier,
            Required required,
            ConstValue value,
            List<TypeAnnotation> annotations)
    {
        this.name = checkNotNull(name, "name");
        this.type = checkNotNull(type, "type");
        this.identifier = Optional.fromNullable(identifier);
        this.required = checkNotNull(required, "required");
        this.value = Optional.fromNullable(value);
        this.annotations = checkNotNull(annotations, "annotations");
    }

    public String getName()
    {
        return name;
    }

    public ThriftType getType()
    {
        return type;
    }

    public Optional<Long> getIdentifier()
    {
        return identifier;
    }

    public Required getRequired()
    {
        return required;
    }

    public Optional<ConstValue> getValue()
    {
        return value;
    }

    public List<TypeAnnotation> getAnnotations()
    {
        return annotations;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("identifier", identifier)
                .add("required", required)
                .add("value", value)
                .add("annotations", annotations)
                .toString();
    }
}
