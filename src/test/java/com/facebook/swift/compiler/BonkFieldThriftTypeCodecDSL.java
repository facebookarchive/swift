/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.compiler.byteCode.ClassDefinition;
import com.facebook.swift.compiler.byteCode.FieldDefinition;
import com.facebook.swift.compiler.byteCode.MethodDefinition;
import com.facebook.swift.compiler.byteCode.ParameterizedType;
import com.google.common.base.Throwables;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

import static com.facebook.swift.compiler.byteCode.Access.BRIDGE;
import static com.facebook.swift.compiler.byteCode.Access.FINAL;
import static com.facebook.swift.compiler.byteCode.Access.PUBLIC;
import static com.facebook.swift.compiler.byteCode.Access.STATIC;
import static com.facebook.swift.compiler.byteCode.Access.SUPER;
import static com.facebook.swift.compiler.byteCode.Access.SYNTHETIC;
import static com.facebook.swift.compiler.byteCode.Access.a;
import static com.facebook.swift.compiler.byteCode.CaseStatement.caseStatement;
import static com.facebook.swift.compiler.byteCode.NamedParameterDefinition.arg;
import static com.facebook.swift.compiler.byteCode.ParameterizedType.type;

public class BonkFieldThriftTypeCodecDSL implements Opcodes
{
    private static final String PACKAGE = "$thrift";
    private static final AtomicLong counter = new AtomicLong();
    private static final boolean debug = true;
    private final DynamicClassLoader classLoader;

    public BonkFieldThriftTypeCodecDSL(DynamicClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    public <T> ThriftTypeCodec<T> genClass(Class<T> structClass)
    {
        String className = PACKAGE + "/" + structClass.getName().replace('.', '/') + "$" + counter.incrementAndGet();
        byte[] byteCode = dump(type(structClass), type(className));

        if (debug) {
            ClassReader reader = new ClassReader(byteCode);
            CheckClassAdapter.verify(reader, true, new PrintWriter(System.out));
        }
        Class<?> codecClass = classLoader.defineClass(className.replace('/', '.'), byteCode);
        try {
            return (ThriftTypeCodec<T>) codecClass.getField("INSTANCE").get(null);
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }


    public static byte[] dump(ParameterizedType structType, ParameterizedType codecType)
    {
        ClassDefinition classDefinition = new ClassDefinition(
                a(PUBLIC, SUPER),
                codecType.getClassName(),
                type(Object.class),
                type(ThriftTypeCodec.class, structType));

        // public static final BonkFieldThriftTypeCodec INSTANCE = new BonkFieldThriftTypeCodec();
        {
            FieldDefinition instanceField = new FieldDefinition(a(PUBLIC, STATIC, FINAL), "INSTANCE", codecType);
            classDefinition.addField(instanceField);

            classDefinition.addMethod(new MethodDefinition(a(STATIC), "<clinit>", type(void.class))
                    .newObject(codecType)
                    .dup()
                    .invokeConstructor(codecType)
                    .putStaticField(codecType, instanceField)
                    .ret()
            );
        }

        // default constructor
        {
            classDefinition.addMethod(new MethodDefinition(a(PUBLIC), "<init>", type(void.class))
                    .loadThis()
                    .invokeConstructor(type(Object.class))
                    .ret());
        }

        // public Class<BonkField> getType()
        {
            classDefinition.addMethod(new MethodDefinition(a(PUBLIC), "getType", type(Class.class, structType))
                    .loadConstant(structType)
                    .retObject());
        }

        // public BonkField read(TProtocolReader protocol) throws Exception
        {
            MethodDefinition read = new MethodDefinition(a(PUBLIC), "read", structType, arg("protocol", TProtocolReader.class))
                    .addException(Exception.class);

            // declare and init local variables here
            read.addStringLocalVariable("message", null);
            read.addIntLocalVariable("type", 0);

            // protocol.readStructBegin();
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readStructBegin", void.class);

            // while (protocol.nextField())
            read.visitLabel("while-begin");
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "nextField", boolean.class);
            read.ifNotGoto("while-end");

            // switch (protocol.getFieldId())
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "getFieldId", short.class);
            read.switchStatement("default", caseStatement(1, "message-field"), caseStatement(2, "type-field"));

            // case 1:
            read.visitLabel("message-field")
                    .loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readString", String.class)
                    .storeVariable("message")
                    .gotoLabel("while-begin");

            // case 2:
            read.visitLabel("type-field")
                    .loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readI32", int.class)
                    .storeVariable("type")
                    .gotoLabel("while-begin");

            // default:
            read.visitLabel("default")
                    .loadVariable("protocol").invokeVirtual(TProtocolReader.class, "skipFieldData", void.class)
                    .gotoLabel("while-begin");

            // end of while loop
            read.visitLabel("while-end");

            // protocol.readStructEnd();
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readStructEnd", void.class);

            // BonkField bonkField = new BonkField();
            read.addLocalVariable("bonkField", structType);
            read.newObject(structType)
                    .dup()
                    .invokeConstructor(structType)
                    .storeVariable("bonkField");

            read.loadVariable("bonkField")
                    .loadVariable("message")
                    .putField(structType, "message", type(String.class));

            read.loadVariable("bonkField")
                    .loadVariable("type")
                    .putField(structType, "type", type(int.class));

            read.loadVariable("bonkField")
                    .retObject();

            classDefinition.addMethod(read);
        }

        // public void write(BonkField struct, TProtocolWriter protocol) throws Exception
        {
            MethodDefinition write = new MethodDefinition(a(PUBLIC), "write", null, arg("struct", structType), arg("protocol", TProtocolWriter.class));
            classDefinition.addMethod(write);

            write.loadVariable("protocol")
                    .loadConstant("bonk")
                    .invokeVirtual(TProtocolWriter.class, "writeStructBegin", void.class, String.class);

            write.loadVariable("protocol")
                    .loadConstant("message")
                    .loadConstant(1)
                    .loadVariable("struct")
                    .getField(structType, "message", type(String.class))
                    .invokeVirtual(TProtocolWriter.class, "writeString", void.class, String.class, short.class, String.class);

            write.loadVariable("protocol")
                    .loadConstant("type")
                    .loadConstant(2)
                    .loadVariable("struct")
                    .getField(structType, "type", type(int.class))
                    .invokeVirtual(TProtocolWriter.class, "writeI32", void.class, String.class, short.class, int.class);

            write.loadVariable("protocol")
                    .invokeVirtual(TProtocolWriter.class, "writeStructEnd", void.class);

            write.ret();
        }

        // public synthetic bridge Object read(TProtocolReader protocol) throws Exception
        {
            classDefinition.addMethod(new MethodDefinition(a(PUBLIC, BRIDGE, SYNTHETIC), "read", type(Object.class), arg("protocol", TProtocolReader.class))
                    .addException(Exception.class)
                    .loadThis()
                    .loadVariable("protocol")
                    .invokeVirtual(codecType, "read", structType, type(TProtocolReader.class))
                    .retObject()
            );
        }

        // public synthetic bridge void write(Object struct, TProtocolWriter protocol) throws Exception
        {
            classDefinition.addMethod(new MethodDefinition(a(PUBLIC, BRIDGE, SYNTHETIC), "write", null, arg("struct", Object.class), arg("protocol", TProtocolWriter.class))
                    .addException(Exception.class)
                    .loadThis()
                    .loadVariable("struct", structType)
                    .loadVariable("protocol")
                    .invokeVirtual(codecType, "write", type(void.class), structType, type(TProtocolWriter.class))
                    .ret()
            );
        }

        ClassNode classNode = classDefinition.getClassNode();

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        return cw.toByteArray();
    }
}
