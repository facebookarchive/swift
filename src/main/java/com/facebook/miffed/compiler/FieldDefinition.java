/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.compiler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.objectweb.asm.tree.FieldNode;

import java.util.EnumSet;

import static com.facebook.miffed.compiler.Access.toAccessModifier;

public class FieldDefinition
{
    private final ImmutableSet<Access> access;
    private final String name;
    private final ParameterizedType type;

    public FieldDefinition(EnumSet<Access> access, String name, ParameterizedType type)
    {
        this.access = Sets.immutableEnumSet(access);
        this.name = name;
        this.type = type;
    }

    public ImmutableSet<Access> getAccess()
    {
        return access;
    }

    public String getName()
    {
        return name;
    }

    public ParameterizedType getType()
    {
        return type;
    }

    public FieldNode getFieldNode()
    {
        return new FieldNode(toAccessModifier(access), name, type.getType(), type.getGenericSignature(), null);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("FieldDefinition");
        sb.append("{access=").append(access);
        sb.append(", name='").append(name).append('\'');
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
