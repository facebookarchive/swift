package com.facebook.swift.parser.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThriftMethod
{
    private final String name;
    private final ThriftType returnType;
    private final List<ThriftField> arguments;
    private final boolean oneway;
    private final List<ThriftField> throwsFields;
    private final List<TypeAnnotation> annotations;

    public ThriftMethod(
            String name,
            ThriftType returnType,
            List<ThriftField> arguments,
            boolean oneway,
            List<ThriftField> throwsFields,
            List<TypeAnnotation> annotations)
    {
        this.name = checkNotNull(name, "name");
        this.returnType = checkNotNull(returnType, "returnType");
        this.arguments = ImmutableList.copyOf(checkNotNull(arguments, "arguments"));
        this.oneway = oneway;
        this.throwsFields = ImmutableList.copyOf(checkNotNull(throwsFields, "throwsFields"));
        this.annotations = ImmutableList.copyOf(checkNotNull(annotations, "annotations"));
    }

    public String getName()
    {
        return name;
    }

    public ThriftType getReturnType()
    {
        return returnType;
    }

    public List<ThriftField> getArguments()
    {
        return arguments;
    }

    public boolean isOneway()
    {
        return oneway;
    }

    public List<ThriftField> getThrowsFields()
    {
        return throwsFields;
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
                .add("returnType", returnType)
                .add("arguments", arguments)
                .add("oneway", oneway)
                .add("throwsFields", throwsFields)
                .add("annotations", annotations)
                .toString();
    }
}
