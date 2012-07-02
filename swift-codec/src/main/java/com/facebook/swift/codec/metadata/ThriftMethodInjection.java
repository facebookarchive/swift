/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.metadata;

import com.google.common.base.Joiner;

import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class ThriftMethodInjection
{
    private final Method method;
    private final List<ThriftParameterInjection> parameters;

    public ThriftMethodInjection(Method method, List<ThriftParameterInjection> parameters)
    {
        checkNotNull(method, "method is null");
        checkNotNull(parameters, "parameters is null");

        this.method = method;
        this.parameters = parameters;
    }

    public Method getMethod()
    {
        return method;
    }

    public List<ThriftParameterInjection> getParameters()
    {
        return parameters;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(method.getName());
        sb.append('(');
        Joiner.on(", ").appendTo(sb, parameters);
        sb.append(')');
        return sb.toString();
    }
}
