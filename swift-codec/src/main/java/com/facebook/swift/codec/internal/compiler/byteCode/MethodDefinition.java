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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import javax.annotation.concurrent.NotThreadSafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.facebook.swift.codec.internal.compiler.byteCode.Access.STATIC;
import static com.facebook.swift.codec.internal.compiler.byteCode.Access.toAccessModifier;
import static com.facebook.swift.codec.internal.compiler.byteCode.NamedParameterDefinition.getNamedParameterType;
import static com.facebook.swift.codec.internal.compiler.byteCode.ParameterizedType.getParameterType;
import static com.facebook.swift.codec.internal.compiler.byteCode.ParameterizedType.toParameterizedType;
import static com.facebook.swift.codec.internal.compiler.byteCode.ParameterizedType.type;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.transform;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SWAP;

@NotThreadSafe
public class MethodDefinition
{
    private final EnumSet<Access> access;
    private final String name;
    private final ParameterizedType returnType;
    private final List<ParameterizedType> parameters;
    private final List<ParameterizedType> exceptions = new ArrayList<>();
    private final InsnList instructionList = new InsnList();

    private final Map<String, LocalVariableDefinition> localVariables = new TreeMap<>();
    private final Map<String, Label> labels = new TreeMap<>();

    private int nextSlot;

    public MethodDefinition(
            EnumSet<Access> access,
            String name,
            ParameterizedType returnType,
            NamedParameterDefinition... parameters)
    {
        this(access, name, returnType, ImmutableList.copyOf(parameters));
    }

    public MethodDefinition(
            EnumSet<Access> access,
            String name,
            ParameterizedType returnType,
            List<NamedParameterDefinition> parameters)
    {
        this.access = access;
        this.name = name;
        if (returnType != null) {
            this.returnType = returnType;
        }
        else {
            this.returnType = type(void.class);
        }
        this.parameters = Lists.transform(parameters, getNamedParameterType());

        if (!access.contains(STATIC)) {
            localVariables.put("this", new LocalVariableDefinition("this", 0, type(Object.class)));
            nextSlot++;
        }
        int argId = 0;
        for (NamedParameterDefinition parameter : parameters) {
            String parameterName = parameter.getName();
            if (parameterName == null) {
                parameterName = "arg" + argId;
            }
            addLocalVariable(parameter.getType(), parameterName);
            argId++;
        }
    }

    public MethodDefinition addException(Class<? extends Throwable> exceptionClass)
    {
        exceptions.add(type(exceptionClass));
        return this;
    }

    public LocalVariableDefinition addLocalVariable(ParameterizedType type, String name)
    {
        Preconditions.checkNotNull(name, "name is null");
        checkArgument(!localVariables.containsKey(name), "There is already a local variable named %s", name);

        LocalVariableDefinition variable = new LocalVariableDefinition(name, nextSlot, type);
        nextSlot += Type.getType(type.getType()).getSize();

        localVariables.put(name, variable);
        return variable;
    }

    public LocalVariableDefinition addInitializedLocalVariable(ParameterizedType type, String name)
    {
        LocalVariableDefinition variable = addLocalVariable(type, name);
        initializeLocalVariable(variable);
        return variable;
    }

    public LocalVariableDefinition getLocalVariable(String name)
    {
        LocalVariableDefinition localVariableDefinition = localVariables.get(name);
        Preconditions.checkArgument(localVariableDefinition != null, "No local variable %s", name);
        return localVariableDefinition;
    }

    public MethodDefinition visitLabel(String name)
    {
        instructionList.add(getLabel(name));
        return this;
    }

    public MethodDefinition gotoLabel(String name)
    {
        instructionList.add(new JumpInsnNode(GOTO, getLabel(name)));
        return this;
    }

    public MethodDefinition ifZeroGoto(String name)
    {
        instructionList.add(new JumpInsnNode(IFEQ, getLabel(name)));
        return this;
    }

    public MethodDefinition ifNullGoto(String name)
    {
        instructionList.add(new JumpInsnNode(IFNULL, getLabel(name)));
        return this;
    }

    public MethodDefinition ifNotNullGoto(String name)
    {
        instructionList.add(new JumpInsnNode(IFNONNULL, getLabel(name)));
        return this;
    }

    private LabelNode getLabel(String name)
    {
        Label label = labels.get(name);
        if (label == null) {
            label = new Label();
            labels.put(name, label);
        }
        return new LabelNode(label);
    }

    public MethodDefinition switchStatement(String defaultCase, CaseStatement... cases)
    {
        switchStatement(defaultCase, ImmutableList.copyOf(cases));
        return this;
    }

    public MethodDefinition switchStatement(String defaultCase, List<CaseStatement> cases)
    {
        LabelNode defaultLabel = getLabel(defaultCase);

        int[] keys = new int[cases.size()];
        LabelNode[] labels = new LabelNode[cases.size()];
        for (int i = 0; i < cases.size(); i++) {
            keys[i] = cases.get(i).getKey();
            labels[i] = getLabel(cases.get(i).getLabel());
        }

        instructionList.add(new LookupSwitchInsnNode(defaultLabel, keys, labels));
        return this;
    }

    public MethodNode getMethodNode()
    {
        MethodNode methodNode = new MethodNode();
        methodNode.access = toAccessModifier(access);
        methodNode.name = name;
        methodNode.desc = methodDescription(returnType, parameters);

        // add generic signature if return type or any parameter is generic
        if (returnType.isGeneric() || any(parameters, ParameterizedType.isGenericType())) {
            methodNode.signature = genericMethodSignature(returnType, parameters);
        }

        methodNode.exceptions = new ArrayList<String>();
        for (ParameterizedType exception : exceptions) {
            methodNode.exceptions.add(exception.getClassName());
        }
        methodNode.instructions.add(instructionList);
        return methodNode;
    }

    public static String methodDescription(Class<?> returnType, Class<?>... parameterTypes)
    {
        return methodDescription(returnType, ImmutableList.copyOf(parameterTypes));
    }

    public static String methodDescription(Class<?> returnType, List<Class<?>> parameterTypes)
    {
        return methodDescription(type(returnType), Lists.transform(parameterTypes, toParameterizedType()));
    }

    public static String methodDescription(ParameterizedType returnType, ParameterizedType... parameterTypes)
    {
        return methodDescription(returnType, ImmutableList.copyOf(parameterTypes));
    }

    public static String methodDescription(ParameterizedType returnType, List<ParameterizedType> parameterTypes)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        Joiner.on("").appendTo(sb, transform(parameterTypes, getParameterType()));
        sb.append(")");
        sb.append(returnType.getType());
        return sb.toString();
    }

    public static String genericMethodSignature(ParameterizedType returnType, ParameterizedType... parameterTypes)
    {
        return genericMethodSignature(returnType, ImmutableList.copyOf(parameterTypes));
    }

    public static String genericMethodSignature(ParameterizedType returnType, List<ParameterizedType> parameterTypes)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        Joiner.on("").appendTo(sb, parameterTypes);
        sb.append(")");
        sb.append(returnType);
        return sb.toString();
    }

    public MethodDefinition loadThis()
    {
        loadObject(0);
        return this;
    }

    public MethodDefinition loadObject(int slot)
    {
        instructionList.add(new VarInsnNode(ALOAD, slot));
        return this;
    }

    public MethodDefinition loadObject(int slot, ParameterizedType type)
    {
        loadObject(slot);
        checkCast(type);
        return this;
    }

    public MethodDefinition checkCast(ParameterizedType type)
    {
        instructionList.add(new TypeInsnNode(CHECKCAST, type.getClassName()));
        return this;
    }

    public MethodDefinition invokeConstructor(Constructor<?> constructor)
    {
        return invokeConstructor(constructor.getDeclaringClass(), constructor.getParameterTypes());
    }

    public MethodDefinition invokeConstructor(Class<?> type, Class<?>... parameterTypes)
    {
        invokeConstructor(
                type(type),
                Lists.transform(ImmutableList.copyOf(parameterTypes), toParameterizedType())
        );
        return this;
    }

    public MethodDefinition invokeConstructor(ParameterizedType type, ParameterizedType... parameterTypes)
    {
        invokeConstructor(type, ImmutableList.copyOf(parameterTypes));
        return this;
    }

    public MethodDefinition invokeConstructor(ParameterizedType type, List<ParameterizedType> parameterTypes)
    {
        invokeSpecial(type, "<init>", type(void.class), parameterTypes);
        return this;
    }

    public MethodDefinition invokeSpecial(
            ParameterizedType type,
            String name,
            ParameterizedType returnType,
            List<ParameterizedType> parameterTypes
    )
    {
        instructionList.add(
                new MethodInsnNode(
                        INVOKESPECIAL,
                        type.getClassName(),
                        name,
                        methodDescription(returnType, parameterTypes)
                )
        );
        return this;
    }

    public MethodDefinition invokeStatic(Method method)
    {
        instructionList.add(
                new MethodInsnNode(
                        INVOKESTATIC,
                        Type.getInternalName(method.getDeclaringClass()),
                        method.getName(),
                        Type.getMethodDescriptor(method)
                )
        );
        return this;
    }

    public MethodDefinition invokeVirtual(Method method)
    {
        instructionList.add(
                new MethodInsnNode(
                        INVOKEVIRTUAL,
                        Type.getInternalName(method.getDeclaringClass()),
                        method.getName(),
                        Type.getMethodDescriptor(method)
                )
        );
        return this;
    }

    public MethodDefinition invokeVirtual(
            Class<?> type,
            String name,
            Class<?> returnType,
            Class<?>... parameterTypes
    )
    {
        instructionList.add(
                new MethodInsnNode(
                        INVOKEVIRTUAL,
                        type(type).getClassName(),
                        name,
                        methodDescription(returnType, parameterTypes)
                )
        );
        return this;
    }

    public MethodDefinition invokeVirtual(
            ParameterizedType type,
            String name,
            ParameterizedType returnType,
            ParameterizedType... parameterTypes
    )
    {
        instructionList.add(
                new MethodInsnNode(
                        INVOKEVIRTUAL,
                        type.getClassName(),
                        name,
                        methodDescription(returnType, parameterTypes)
                )
        );
        return this;
    }

    public MethodDefinition ret()
    {
        instructionList.add(new InsnNode(RETURN));
        return this;
    }

    public MethodDefinition retObject()
    {
        instructionList.add(new InsnNode(ARETURN));
        return this;
    }

    public MethodDefinition throwException()
    {
        instructionList.add(new InsnNode(ATHROW));
        return this;
    }

    public MethodDefinition newObject(Class<?> type)
    {
        instructionList.add(new TypeInsnNode(NEW, type(type).getClassName()));
        return this;
    }

    public MethodDefinition newObject(ParameterizedType type)
    {
        instructionList.add(new TypeInsnNode(NEW, type.getClassName()));
        return this;
    }

    public MethodDefinition dup()
    {
        instructionList.add(new InsnNode(DUP));
        return this;
    }

    public MethodDefinition pop()
    {
        instructionList.add(new InsnNode(POP));
        return this;
    }

    public MethodDefinition swap()
    {
        instructionList.add(new InsnNode(SWAP));
        return this;
    }

    public MethodDefinition getField(Field field)
    {
        return getField(field.getDeclaringClass(), field.getName(), field.getType());
    }

    public MethodDefinition getField(Class<?> target, FieldDefinition field)
    {
        getField(type(target), field.getName(), field.getType());
        return this;
    }

    public MethodDefinition getField(ParameterizedType target, FieldDefinition field)
    {
        getField(target, field.getName(), field.getType());
        return this;
    }

    public MethodDefinition getField(Class<?> target, String fieldName, Class<?> fieldType)
    {
        getField(type(target), fieldName, type(fieldType));
        return this;
    }

    public MethodDefinition getField(ParameterizedType target, String fieldName, ParameterizedType fieldType)
    {
        instructionList.add(
                new FieldInsnNode(
                        GETFIELD,
                        target.getClassName(),
                        fieldName,
                        fieldType.getType()
                )
        );
        return this;
    }

    public MethodDefinition getStaticField(ParameterizedType target, FieldDefinition field)
    {
        checkArgument(field.getAccess().contains(STATIC), "Field is not static: %s", field);
        getStaticField(target, field.getName(), field.getType());
        return this;
    }

    public MethodDefinition getStaticField(ParameterizedType target, String fieldName, ParameterizedType fieldType)
    {
        instructionList.add(
                new FieldInsnNode(
                        GETSTATIC,
                        target.getClassName(),
                        fieldName,
                        fieldType.getType()
                )
        );
        return this;
    }

    public MethodDefinition putStaticField(ParameterizedType target, FieldDefinition field)
    {
        checkArgument(field.getAccess().contains(STATIC), "Field is not static: %s", field);
        putStaticField(target, field.getName(), field.getType());
        return this;
    }

    public MethodDefinition putStaticField(ParameterizedType target, String fieldName, ParameterizedType fieldType)
    {
        instructionList.add(
                new FieldInsnNode(
                        PUTSTATIC,
                        target.getClassName(),
                        fieldName,
                        fieldType.getType()
                )
        );
        return this;
    }

    public MethodDefinition putField(Field field)
    {
        return putField(field.getDeclaringClass(), field.getName(), field.getType());
    }

    public MethodDefinition putField(Class<?> target, FieldDefinition field)
    {
        return putField(type(target), field);
    }

    public MethodDefinition putField(Class<?> target, String fieldName, Class<?> fieldType)
    {
        this.putField(type(target), fieldName, type(fieldType));
        return this;
    }

    public MethodDefinition putField(ParameterizedType target, FieldDefinition field)
    {
        checkArgument(!field.getAccess().contains(STATIC), "Field is static: %s", field);

        instructionList.add(
                new FieldInsnNode(
                        PUTFIELD,
                        target.getClassName(),
                        field.getName(),
                        field.getType().getType()
                )
        );
        return this;
    }

    public MethodDefinition putField(ParameterizedType target, String fieldName, ParameterizedType fieldType)
    {
        instructionList.add(
                new FieldInsnNode(
                        PUTFIELD,
                        target.getClassName(),
                        fieldName,
                        fieldType.getType()
                )
        );
        return this;
    }

    public MethodDefinition loadNull()
    {
        instructionList.add(new InsnNode(ACONST_NULL));
        return this;
    }

    public MethodDefinition addInstruction(AbstractInsnNode node)
    {
        instructionList.add(node);
        return this;
    }

    public MethodDefinition loadConstant(Class<?> type)
    {
        instructionList.add(new LdcInsnNode(Type.getType(type)));
        return this;
    }

    public MethodDefinition loadConstant(ParameterizedType type)
    {
        instructionList.add(new LdcInsnNode(Type.getType(type.getType())));
        return this;
    }

    public MethodDefinition loadConstant(String value)
    {
        instructionList.add(new LdcInsnNode(value));
        return this;
    }

    public MethodDefinition loadConstant(int value)
    {
        switch (value) {
            case -1:
                instructionList.add(new InsnNode(ICONST_M1));
                break;
            case 0:
                instructionList.add(new InsnNode(ICONST_0));
                break;
            case 1:
                instructionList.add(new InsnNode(ICONST_1));
                break;
            case 2:
                instructionList.add(new InsnNode(ICONST_2));
                break;
            case 3:
                instructionList.add(new InsnNode(ICONST_3));
                break;
            case 4:
                instructionList.add(new InsnNode(ICONST_4));
                break;
            case 5:
                instructionList.add(new InsnNode(ICONST_5));
                break;
            default:
                instructionList.add(new LdcInsnNode(value));
                break;
        }
        return this;
    }

    public MethodDefinition loadVariable(String name)
    {
        LocalVariableDefinition variable = localVariables.get(name);
        checkArgument(variable != null, "unknown variable %s", name);
        loadVariable(variable);
        return this;
    }

    public MethodDefinition loadVariable(String name, ParameterizedType type)
    {
        loadVariable(name);
        checkCast(type);
        return this;
    }

    public MethodDefinition initializeLocalVariable(LocalVariableDefinition variable)
    {
        ParameterizedType type = variable.getType();
        if (type.getType().length() == 1) {
            switch (type.getType().charAt(0)) {
                case 'B':
                case 'Z':
                case 'S':
                case 'C':
                case 'I':
                    instructionList.add(new InsnNode(ICONST_0));
                    break;
                case 'F':
                    instructionList.add(new InsnNode(FCONST_0));
                    break;
                case 'D':
                    instructionList.add(new InsnNode(DCONST_0));
                    break;
                case 'J':
                    instructionList.add(new InsnNode(LCONST_0));
                    break;
                default:
                    checkArgument(false, "Unknown type '%s'", variable.getType());
            }
        }
        else {
            instructionList.add(new InsnNode(ACONST_NULL));
        }

        instructionList.add(new VarInsnNode(Type.getType(type.getType()).getOpcode(ISTORE), variable.getSlot()));

        return this;
    }

    public MethodDefinition loadVariable(LocalVariableDefinition variable)
    {
        ParameterizedType type = variable.getType();
        instructionList.add(new VarInsnNode(Type.getType(type.getType()).getOpcode(ILOAD), variable.getSlot()));
        return this;
    }

    public MethodDefinition storeVariable(String name)
    {
        LocalVariableDefinition variable = localVariables.get(name);
        checkArgument(variable != null, "unknown variable %s" + name);
        storeVariable(variable);
        return this;
    }

    public MethodDefinition storeVariable(LocalVariableDefinition variable)
    {
        ParameterizedType type = variable.getType();
        instructionList.add(new VarInsnNode(Type.getType(type.getType()).getOpcode(ISTORE), variable.getSlot()));
        return this;
    }
}
