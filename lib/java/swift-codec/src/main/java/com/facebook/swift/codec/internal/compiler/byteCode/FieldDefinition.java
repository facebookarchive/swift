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
package com.facebook.swift.codec.internal.compiler.byteCode;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.objectweb.asm.tree.FieldNode;

import javax.annotation.concurrent.Immutable;
import java.util.EnumSet;

import static com.facebook.swift.codec.internal.compiler.byteCode.Access.toAccessModifier;

@Immutable
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
        return new FieldNode(
                toAccessModifier(access),
                name,
                type.getType(),
                type.getGenericSignature(),
                null);
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
