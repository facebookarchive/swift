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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.facebook.swift.codec.internal.compiler.byteCode.Access.toAccessModifier;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.concat;
import static org.objectweb.asm.Opcodes.V1_6;

@NotThreadSafe
public class ClassDefinition
{
    private final int access;
    private final String name;
    private final ParameterizedType superClass;
    private final List<ParameterizedType> interfaces = new ArrayList<>();
    private final List<FieldDefinition> fields = new ArrayList<>();
    private final List<MethodDefinition> methods = new ArrayList<>();

    public ClassDefinition(
            EnumSet<Access> access,
            String name,
            ParameterizedType superClass,
            ParameterizedType... interfaces)
    {
        this.access = toAccessModifier(access);
        this.name = name;
        this.superClass = superClass;
        this.interfaces.addAll(ImmutableList.copyOf(interfaces));
    }

    public ClassNode getClassNode()
    {
        ClassNode classNode = new ClassNode();
        classNode.version = V1_6;

        classNode.access = access;

        classNode.name = name;

        classNode.superName = superClass.getClassName();
        for (ParameterizedType interfaceType : interfaces) {
            classNode.interfaces.add(interfaceType.getClassName());
        }

        // add generic signature if super class or any interface is generic
        if (superClass.isGeneric() || any(interfaces, ParameterizedType.isGenericType())) {
            classNode.signature = genericClassSignature(superClass, interfaces);
        }

        for (FieldDefinition field : fields) {
            classNode.fields.add(field.getFieldNode());
        }

        for (MethodDefinition method : methods) {
            classNode.methods.add(method.getMethodNode());
        }

        return classNode;
    }

    public ClassDefinition addField(EnumSet<Access> access, String name, ParameterizedType type)
    {
        fields.add(new FieldDefinition(access, name, type));
        return this;
    }

    public ClassDefinition addField(FieldDefinition field)
    {
        fields.add(field);
        return this;
    }

    public ClassDefinition addMethod(MethodDefinition method)
    {
        methods.add(method);
        return this;
    }

    public static String genericClassSignature(
            ParameterizedType classType,
            ParameterizedType... interfaceTypes
    )
    {
        return Joiner.on("").join(
                concat(ImmutableList.of(classType), ImmutableList.copyOf(interfaceTypes))
        );
    }

    public static String genericClassSignature(
            ParameterizedType classType,
            List<ParameterizedType> interfaceTypes
    )
    {
        return Joiner.on("").join(concat(ImmutableList.of(classType), interfaceTypes));
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ClassDefinition");
        sb.append("{access=").append(access);
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
