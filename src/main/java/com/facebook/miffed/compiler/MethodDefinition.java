/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.compiler;

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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.facebook.miffed.compiler.Access.STATIC;
import static com.facebook.miffed.compiler.Access.toAccessModifier;
import static com.facebook.miffed.compiler.NamedParameterDefinition.getNamedParameterType;
import static com.facebook.miffed.compiler.ParameterizedType.getParameterType;
import static com.facebook.miffed.compiler.ParameterizedType.isGenericType;
import static com.facebook.miffed.compiler.ParameterizedType.toParameterizedType;
import static com.facebook.miffed.compiler.ParameterizedType.type;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.transform;
import static org.objectweb.asm.Opcodes.*;

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

    public MethodDefinition(EnumSet<Access> access, String name, ParameterizedType returnType, NamedParameterDefinition... parameters)
    {
        this(access, name, returnType, ImmutableList.copyOf(parameters));
    }

    public MethodDefinition(EnumSet<Access> access, String name, ParameterizedType returnType, List<NamedParameterDefinition> parameters)
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
            localVariables.put(parameterName, new LocalVariableDefinition(parameterName, localVariables.size(), parameter.getType()));
            argId++;
            nextSlot++;
        }
    }


    public MethodDefinition addException(Class<? extends Throwable> exceptionClass)
    {
        exceptions.add(type(exceptionClass));
        return this;
    }

    public LocalVariableDefinition getLocalVariable(String name)
    {
        return localVariables.get(name);
    }

    public LocalVariableDefinition addLocalVariable(String name, Class<?> type)
    {
        return addLocalVariable(name, type(type));
    }

    public LocalVariableDefinition addLocalVariable(String name, ParameterizedType type)
    {
        Preconditions.checkNotNull(name, "name is null");
        Preconditions.checkArgument(!localVariables.containsKey(name), "There is already a local variable named %s", name);

        LocalVariableDefinition variable = new LocalVariableDefinition(name, nextSlot++, type);

        // longs and doubles take up two slots
        if (variable.getType().getType().equals("D") || variable.getType().getType().equals("L")) {
            nextSlot++;
        }

        localVariables.put(name, variable);
        return variable;
    }

    public LocalVariableDefinition addStringLocalVariable(String name, String value)
    {
        LocalVariableDefinition variable = addLocalVariable(name, type(String.class));
        if (value == null) {
            loadNull();
        }
        else {
            loadConstant(value);
        }
        storeVariable(variable);
        return variable;
    }

    public LocalVariableDefinition addIntLocalVariable(String name, int value)
    {
        LocalVariableDefinition variable = addLocalVariable(name, type(int.class));
        loadConstant(value);
        storeVariable(variable);
        return variable;
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

    public MethodDefinition ifNotGoto(String name)
    {
        instructionList.add(new JumpInsnNode(IFEQ, getLabel(name)));
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
        if (returnType.isGeneric() || any(parameters, isGenericType())) {
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
        this.instructionList.add(new TypeInsnNode(CHECKCAST, type.getClassName()));
        return this;
    }

    public MethodDefinition invokeConstructor(Class<?> type, Class<?>... parameterTypes)
    {
        invokeConstructor(type(type), Lists.transform(ImmutableList.copyOf(parameterTypes), toParameterizedType()));
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

    public MethodDefinition invokeSpecial(ParameterizedType type, String name, ParameterizedType returnType, List<ParameterizedType> parameterTypes)
    {
        this.instructionList.add(new MethodInsnNode(INVOKESPECIAL, type.getClassName(), name, methodDescription(returnType, parameterTypes)));
        return this;
    }

    public MethodDefinition invokeVirtual(Class<?> type, String name, Class<?> returnType, Class<?>... parameterTypes)
    {
        this.instructionList.add(new MethodInsnNode(INVOKEVIRTUAL, type(type).getClassName(), name, methodDescription(returnType, parameterTypes)));
        return this;
    }

    public MethodDefinition invokeVirtual(ParameterizedType type, String name, ParameterizedType returnType, ParameterizedType... parameterTypes)
    {
        this.instructionList.add(new MethodInsnNode(INVOKEVIRTUAL, type.getClassName(), name, methodDescription(returnType, parameterTypes)));
        return this;
    }

    public MethodDefinition ret()
    {
        this.instructionList.add(new InsnNode(RETURN));
        return this;
    }

    public MethodDefinition retObject()
    {
        this.instructionList.add(new InsnNode(ARETURN));
        return this;
    }

    public MethodDefinition newObject(Class<?> type)
    {
        this.instructionList.add(new TypeInsnNode(NEW, type(type).getClassName()));
        return this;
    }

    public MethodDefinition newObject(ParameterizedType type)
    {
        this.instructionList.add(new TypeInsnNode(NEW, type.getClassName()));
        return this;
    }

    public MethodDefinition dup()
    {
        this.instructionList.add(new InsnNode(DUP));
        return this;
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
        instructionList.add(new FieldInsnNode(GETFIELD, target.getClassName(), fieldName, fieldType.getType()));
        return this;
    }

    public MethodDefinition putStaticField(ParameterizedType target, FieldDefinition field)
    {
        Preconditions.checkArgument(field.getAccess().contains(STATIC), "Field is not static: %s", field);

        this.instructionList.add(new FieldInsnNode(PUTSTATIC, target.getClassName(), field.getName(), field.getType().getType()));
        return this;
    }

    public MethodDefinition putStaticField(ParameterizedType target, String fieldName, ParameterizedType fieldType)
    {
        this.instructionList.add(new FieldInsnNode(PUTSTATIC, target.getClassName(), fieldName, fieldType.getType()));
        return this;
    }

    public MethodDefinition putField(Class<?> target, FieldDefinition field)
    {
        this.putField(type(target), field);
        return this;
    }

    public MethodDefinition putField(Class<?> target, String fieldName, Class<?> fieldType)
    {
        this.putField(type(target), fieldName, type(fieldType));
        return this;
    }

    public MethodDefinition putField(ParameterizedType target, FieldDefinition field)
    {
        Preconditions.checkArgument(!field.getAccess().contains(STATIC), "Field is static: %s", field);

        this.instructionList.add(new FieldInsnNode(PUTFIELD, target.getClassName(), field.getName(), field.getType().getType()));
        return this;
    }

    public MethodDefinition putField(ParameterizedType target, String fieldName, ParameterizedType fieldType)
    {
        this.instructionList.add(new FieldInsnNode(PUTFIELD, target.getClassName(), fieldName, fieldType.getType()));
        return this;
    }

    public MethodDefinition loadNull()
    {
        this.instructionList.add(new InsnNode(ACONST_NULL));
        return this;
    }

    public MethodDefinition addInstruction(AbstractInsnNode node)
    {
        instructionList.add(node);
        return this;
    }

    public MethodDefinition loadConstant(Class<?> type)
    {
        this.instructionList.add(new LdcInsnNode(Type.getType(type)));
        return this;
    }

    public MethodDefinition loadConstant(ParameterizedType type)
    {
        this.instructionList.add(new LdcInsnNode(Type.getType(type.getType())));
        return this;
    }

    public MethodDefinition loadConstant(String value)
    {
        this.instructionList.add(new LdcInsnNode(value));
        return this;
    }

    public MethodDefinition loadConstant(int value)
    {
        switch (value) {
            case -1:
                this.instructionList.add(new InsnNode(ICONST_M1));
                break;
            case 0:
                this.instructionList.add(new InsnNode(ICONST_0));
                break;
            case 1:
                this.instructionList.add(new InsnNode(ICONST_1));
                break;
            case 2:
                this.instructionList.add(new InsnNode(ICONST_2));
                break;
            case 3:
                this.instructionList.add(new InsnNode(ICONST_3));
                break;
            case 4:
                this.instructionList.add(new InsnNode(ICONST_4));
                break;
            case 5:
                this.instructionList.add(new InsnNode(ICONST_5));
                break;
            default:
                this.instructionList.add(new LdcInsnNode(value));
                break;
        }
        return this;
    }

    public MethodDefinition loadVariable(String name)
    {
        LocalVariableDefinition variable = localVariables.get(name);
        Preconditions.checkArgument(variable != null, "unknown variable %s" + name);
        loadVariable(variable);
        return this;
    }

    public MethodDefinition loadVariable(String name, ParameterizedType type)
    {
        loadVariable(name);
        checkCast(type);
        return this;
    }


    public MethodDefinition loadVariable(LocalVariableDefinition variable)
    {
        ParameterizedType type = variable.getType();
        if (type.getType().length() == 1) {
            switch (type.getType().charAt(0)) {
                case 'B':
                case 'Z':
                case 'S':
                case 'C':
                case 'I':
                    instructionList.add(new VarInsnNode(ILOAD, variable.getSlot()));
                    break;
                case 'F':
                    instructionList.add(new VarInsnNode(FLOAD, variable.getSlot()));
                    break;
                case 'D':
                    instructionList.add(new VarInsnNode(DLOAD, variable.getSlot()));
                    break;
                case 'J':
                    instructionList.add(new VarInsnNode(LLOAD, variable.getSlot()));
                    break;
                default:
                    Preconditions.checkArgument(false, "Unknown type '%s'", variable.getType());
            }
        }
        else {
            instructionList.add(new VarInsnNode(ALOAD, variable.getSlot()));
        }
        return this;
    }

    public MethodDefinition storeVariable(String name)
    {
        LocalVariableDefinition variable = localVariables.get(name);
        Preconditions.checkArgument(variable != null, "unknown variable %s" + name);
        storeVariable(variable);
        return this;
    }

    public MethodDefinition storeVariable(LocalVariableDefinition variable)
    {
        ParameterizedType type = variable.getType();
        if (type.getType().length() == 1) {
            switch (type.getType().charAt(0)) {
                case 'B':
                case 'Z':
                case 'S':
                case 'C':
                case 'I':
                    instructionList.add(new VarInsnNode(ISTORE, variable.getSlot()));
                    break;
                case 'F':
                    instructionList.add(new VarInsnNode(FSTORE, variable.getSlot()));
                    break;
                case 'D':
                    instructionList.add(new VarInsnNode(DSTORE, variable.getSlot()));
                    break;
                case 'J':
                    instructionList.add(new VarInsnNode(LSTORE, variable.getSlot()));
                    break;
                default:
                    Preconditions.checkArgument(false, "Unknown type '%s'", variable.getType());
            }
        }
        else {
            instructionList.add(new VarInsnNode(ASTORE, variable.getSlot()));
        }
        return this;
    }
}
